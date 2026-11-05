-- Centralized staff PIN authentication.
-- PIN hashes are never readable through PostgREST; all access goes through SECURITY DEFINER RPCs.
CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA extensions;

CREATE TABLE IF NOT EXISTS public.staff_members (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  display_name TEXT NOT NULL CHECK (length(trim(display_name)) BETWEEN 1 AND 80),
  role TEXT NOT NULL CHECK (role IN ('cashier', 'manager')),
  pin_hash TEXT NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_by UUID NOT NULL REFERENCES auth.users(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS staff_members_active_name_idx
  ON public.staff_members (lower(display_name)) WHERE is_active;

CREATE TABLE IF NOT EXISTS public.staff_pin_attempts (
  user_id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  failed_count INTEGER NOT NULL DEFAULT 0,
  window_started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  locked_until TIMESTAMPTZ
);

ALTER TABLE public.staff_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.staff_pin_attempts ENABLE ROW LEVEL SECURITY;

REVOKE ALL ON TABLE public.staff_members, public.staff_pin_attempts FROM PUBLIC, anon, authenticated;

CREATE OR REPLACE FUNCTION public.list_staff_members()
RETURNS TABLE(id UUID, display_name TEXT, role TEXT, is_active BOOLEAN)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
  IF NOT public.is_store_admin() THEN
    RAISE EXCEPTION 'not authorized' USING ERRCODE = '42501';
  END IF;
  RETURN QUERY
    SELECT s.id, s.display_name, s.role, s.is_active
    FROM public.staff_members AS s
    ORDER BY s.display_name;
END;
$$;

CREATE OR REPLACE FUNCTION public.save_staff_member(
  p_display_name TEXT,
  p_role TEXT,
  p_pin TEXT,
  p_id UUID DEFAULT NULL
)
RETURNS UUID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
  v_id UUID;
BEGIN
  IF NOT public.is_store_admin() THEN
    RAISE EXCEPTION 'not authorized' USING ERRCODE = '42501';
  END IF;
  IF length(trim(COALESCE(p_display_name, ''))) NOT BETWEEN 1 AND 80
     OR p_role NOT IN ('cashier', 'manager')
     OR p_pin !~ '^[0-9]{4,8}$' THEN
    RAISE EXCEPTION 'invalid staff details' USING ERRCODE = '22023';
  END IF;

  IF p_id IS NULL THEN
    INSERT INTO public.staff_members(display_name, role, pin_hash, created_by)
    VALUES (trim(p_display_name), p_role, extensions.crypt(p_pin, extensions.gen_salt('bf', 12)), auth.uid())
    RETURNING id INTO v_id;
  ELSE
    UPDATE public.staff_members
    SET display_name = trim(p_display_name),
        role = p_role,
        pin_hash = extensions.crypt(p_pin, extensions.gen_salt('bf', 12)),
        updated_at = NOW()
    WHERE id = p_id
    RETURNING id INTO v_id;
    IF v_id IS NULL THEN
      RAISE EXCEPTION 'staff member not found' USING ERRCODE = 'P0002';
    END IF;
  END IF;
  RETURN v_id;
END;
$$;

CREATE OR REPLACE FUNCTION public.set_staff_active(p_id UUID, p_is_active BOOLEAN)
RETURNS BOOLEAN
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
  IF NOT public.is_store_admin() THEN
    RAISE EXCEPTION 'not authorized' USING ERRCODE = '42501';
  END IF;
  UPDATE public.staff_members
  SET is_active = p_is_active, updated_at = NOW()
  WHERE id = p_id;
  RETURN FOUND;
END;
$$;

CREATE OR REPLACE FUNCTION public.verify_staff_pin(p_pin TEXT)
RETURNS TABLE(id UUID, display_name TEXT, role TEXT)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
  v_user UUID := auth.uid();
  v_attempts public.staff_pin_attempts%ROWTYPE;
  v_staff public.staff_members%ROWTYPE;
BEGIN
  IF v_user IS NULL OR NOT public.is_store_admin() THEN
    RAISE EXCEPTION 'not authorized' USING ERRCODE = '42501';
  END IF;

  INSERT INTO public.staff_pin_attempts(user_id)
  VALUES (v_user)
  ON CONFLICT (user_id) DO NOTHING;

  SELECT * INTO v_attempts
  FROM public.staff_pin_attempts
  WHERE user_id = v_user
  FOR UPDATE;

  IF v_attempts.locked_until IS NOT NULL AND v_attempts.locked_until > NOW() THEN
    RAISE EXCEPTION 'too many attempts; try again later' USING ERRCODE = 'P0001';
  END IF;

  IF v_attempts.window_started_at < NOW() - INTERVAL '15 minutes' THEN
    UPDATE public.staff_pin_attempts
    SET failed_count = 0, window_started_at = NOW(), locked_until = NULL
    WHERE user_id = v_user;
  END IF;

  SELECT * INTO v_staff
  FROM public.staff_members
  WHERE is_active
    AND p_pin ~ '^[0-9]{4,8}$'
    AND pin_hash = extensions.crypt(p_pin, pin_hash)
  LIMIT 1;

  IF NOT FOUND THEN
    UPDATE public.staff_pin_attempts
    SET failed_count = CASE
          WHEN window_started_at < NOW() - INTERVAL '15 minutes' THEN 1
          ELSE failed_count + 1
        END,
        window_started_at = CASE
          WHEN window_started_at < NOW() - INTERVAL '15 minutes' THEN NOW()
          ELSE window_started_at
        END,
        locked_until = CASE
          WHEN (CASE
            WHEN window_started_at < NOW() - INTERVAL '15 minutes' THEN 1
            ELSE failed_count + 1
          END) >= 5 THEN NOW() + INTERVAL '15 minutes'
          ELSE NULL
        END
    WHERE user_id = v_user;
    RETURN;
  END IF;

  DELETE FROM public.staff_pin_attempts WHERE user_id = v_user;
  RETURN QUERY SELECT v_staff.id, v_staff.display_name, v_staff.role;
END;
$$;

REVOKE ALL ON FUNCTION public.list_staff_members() FROM PUBLIC, anon;
REVOKE ALL ON FUNCTION public.save_staff_member(TEXT, TEXT, TEXT, UUID) FROM PUBLIC, anon;
REVOKE ALL ON FUNCTION public.set_staff_active(UUID, BOOLEAN) FROM PUBLIC, anon;
REVOKE ALL ON FUNCTION public.verify_staff_pin(TEXT) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.list_staff_members() TO authenticated;
GRANT EXECUTE ON FUNCTION public.save_staff_member(TEXT, TEXT, TEXT, UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION public.set_staff_active(UUID, BOOLEAN) TO authenticated;
GRANT EXECUTE ON FUNCTION public.verify_staff_pin(TEXT) TO authenticated;
