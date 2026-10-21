with open('web/components/DashboardLayout.js', 'r') as f:
    text = f.read()

if 'useTheme' not in text:
    old_import = "import { supabase } from '@/lib/supabase/client'"
    new_import = "import { supabase } from '@/lib/supabase/client'\nimport { useTheme } from '@/lib/context/ThemeContext'"
    text = text.replace(old_import, new_import)

if 'const { language, setLanguage } = useTheme()' not in text:
    old_fn = "export default function DashboardLayout({ children }) {\n  const router = useRouter()"
    new_fn = "export default function DashboardLayout({ children }) {\n  const router = useRouter()\n  const { language, setLanguage } = useTheme()"
    text = text.replace(old_fn, new_fn)

old_header = '<div className="flex items-center gap-3">\n            <div className="flex items-center gap-2 px-3 py-1.5 rounded-xl bg-emerald-50 text-emerald-700 border border-emerald-100 text-xs font-semibold">'
new_header = '''<div className="flex items-center gap-3">
            <div className="flex items-center gap-1 bg-slate-100 p-1 rounded-xl border border-slate-200 text-xs font-bold">
              {[
                { code: 'es', label: 'ES' },
                { code: 'en', label: 'EN' },
                { code: 'he', label: 'HE' },
                { code: 'pt', label: 'PT' },
                { code: 'fr', label: 'FR' },
                { code: 'de', label: 'DE' },
                { code: 'zh', label: 'ZH' },
                { code: 'ja', label: 'JA' },
                { code: 'ar', label: 'AR' },
              ].map((lang) => (
                <button
                  key={lang.code}
                  onClick={() => setLanguage(lang.code)}
                  className={`px-2 py-1 rounded-lg transition-all ${
                    language === lang.code
                      ? 'bg-sky-600 text-white shadow'
                      : 'text-slate-600 hover:text-slate-900 hover:bg-slate-200'
                  }`}
                >
                  {lang.label}
                </button>
              ))}
            </div>
            <div className="flex items-center gap-2 px-3 py-1.5 rounded-xl bg-emerald-50 text-emerald-700 border border-emerald-100 text-xs font-semibold">'''

if old_header in text:
    text = text.replace(old_header, new_header)

with open('web/components/DashboardLayout.js', 'w') as f:
    f.write(text)
print("Updated DashboardLayout.js successfully")
