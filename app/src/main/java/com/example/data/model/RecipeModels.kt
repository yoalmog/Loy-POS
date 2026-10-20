package com.example.data.model

import java.util.UUID

enum class KitchenStation(val labelHebrew: String, val labelEnglish: String, val labelSpanish: String) {
    GRILL("גריל וצלייה", "Grill & BBQ", "Parrilla y Asados"),
    WOK("ווק ומוקפצים", "Wok & Stir Fry", "Wok y Salteados"),
    OVEN("טאבון ופיצות", "Oven & Pizza", "Horno y Pizzas"),
    SALAD("פס קר וסלטים", "Cold & Salads", "Fríos y Ensaladas"),
    BAR("בר ומשקאות", "Bar & Drinks", "Bar y Bebidas"),
    PASTRY("קינוחים ומתוקים", "Pastry & Desserts", "Postres y Dulces"),
    GENERAL("מטבח כללי", "General Kitchen", "Cocina General")
}

data class RecipeIngredient(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val quantity: Double,
    val unit: String = "גרם", // גרם, ק"ג, מ"ל, ליטר, יח', כפות
    val costPerUnit: Double = 0.0,
    val deductStock: Boolean = true
)

data class KitchenPrepGuide(
    val prepTimeMinutes: Int = 10,
    val station: KitchenStation = KitchenStation.GENERAL,
    val steps: List<String> = emptyList(),
    val allergens: List<String> = emptyList(),
    val chefNotes: String = "",
    val platingGuide: String = ""
)

data class DishRecipe(
    val itemId: Long,
    val itemName: String,
    val categoryName: String = "כללי",
    val ingredients: List<RecipeIngredient> = emptyList(),
    val prepGuide: KitchenPrepGuide = KitchenPrepGuide()
) {
    val totalFoodCost: Double
        get() = ingredients.sumOf { it.quantity * it.costPerUnit }
}

enum class KitchenTicketStatus(val labelHebrew: String, val labelSpanish: String, val colorHex: Long) {
    NEW("חדש", "Nueva", 0xFF0288D1),
    PREPARING("בהכנה", "En Preparación", 0xFFF59E0B),
    READY("מוכן להגשה", "Lista", 0xFF10B981),
    COMPLETED("הוגש", "Completada", 0xFF64748B)
}

data class KitchenOrderItem(
    val id: Long,
    val name: String,
    val quantity: Int = 1,
    val notes: String = "",
    val recipe: DishRecipe? = null,
    val isCompleted: Boolean = false
)

data class KitchenOrderTicket(
    val id: String = UUID.randomUUID().toString(),
    val orderNumber: Long,
    val tableName: String = "Takeout",
    val serverName: String = "קופאי",
    val timestamp: Long = System.currentTimeMillis(),
    val items: List<KitchenOrderItem> = emptyList(),
    val status: KitchenTicketStatus = KitchenTicketStatus.NEW
)

object DefaultRecipesSeed {
    fun getDefaultSeedRecipes(): Map<Long, DishRecipe> {
        return mapOf(
            // Lomo Saltado
            101L to DishRecipe(
                itemId = 101L,
                itemName = "Lomo Saltado Criollo",
                categoryName = "Platos de Fondo",
                ingredients = listOf(
                    RecipeIngredient(name = "רצועות לוקוס / בקר אנטרקוט", quantity = 220.0, unit = "גרם", costPerUnit = 0.06),
                    RecipeIngredient(name = "בצל סגול פרוס גס", quantity = 80.0, unit = "גרם", costPerUnit = 0.01),
                    RecipeIngredient(name = "עגבניות שרי / פלחי עגבנייה", quantity = 70.0, unit = "גרם", costPerUnit = 0.012),
                    RecipeIngredient(name = "צ'יפס תפוחי אדמה קריספי", quantity = 150.0, unit = "גרם", costPerUnit = 0.008),
                    RecipeIngredient(name = "רוטב סויה וחומץ פילפל צ'ילי", quantity = 35.0, unit = "מ\"ל", costPerUnit = 0.03),
                    RecipeIngredient(name = "אורז יסמין מאודה", quantity = 180.0, unit = "גרם", costPerUnit = 0.005),
                    RecipeIngredient(name = "כוסברה טרייה קצוצה", quantity = 10.0, unit = "גרם", costPerUnit = 0.02)
                ),
                prepGuide = KitchenPrepGuide(
                    prepTimeMinutes = 12,
                    station = KitchenStation.WOK,
                    steps = listOf(
                        "לחמם ווק פלדה עד להעלת עשן קלה, להוסיף 2 כפות שמן ניטרלי.",
                        "לצרוב את נתחי הבקר בחום גבוה מאוד 90 שניות לקבלת אפקט ווק-היי (Wok Hei).",
                        "להוסיף את רצועות הבצל והפלפל ולהקפיץ במהירות 60 שניות.",
                        "להוסיף עגבניות, רוטב סויה עשיר וחומץ, ולהקפיץ לקרמול עמוק.",
                        "לשלב חצי מכמות הצ'יפס החם לתוך הווק לערבוב של 15 שניות.",
                        "צלחות: ערימת אורז יסמין בצד אחד, המוקפץ לצדו, ופיזור כוסברה רעננה מעל."
                    ),
                    allergens = listOf("סויה", "גלוטן"),
                    chefNotes = "חום הווק חייב להיות מקסימלי כדי למנוע הצטברות נוזלים ולשמור על פריכות הבצל.",
                    platingGuide = "צלחת עמוקה לבנה, קערת אורז מעוצבת ופילחי עגבניה שלמים מעל."
                )
            ),
            // Ceviche Clasico
            102L to DishRecipe(
                itemId = 102L,
                itemName = "Ceviche Clásico de Pescado",
                categoryName = "Pescados y Mariscos",
                ingredients = listOf(
                    RecipeIngredient(name = "דג ים לבן טרי מפולט", quantity = 200.0, unit = "גרם", costPerUnit = 0.09),
                    RecipeIngredient(name = "מיץ ליים טרי סחוט", quantity = 60.0, unit = "מ\"ל", costPerUnit = 0.02),
                    RecipeIngredient(name = "בצל סגול פרוס דק (Julienne)", quantity = 40.0, unit = "גרם", costPerUnit = 0.01),
                    RecipeIngredient(name = "צ'ילי חריף (Ají Limo)", quantity = 5.0, unit = "גרם", costPerUnit = 0.05),
                    RecipeIngredient(name = "בטטה מזוגגת בדבש וקינמון", quantity = 90.0, unit = "גרם", costPerUnit = 0.008),
                    RecipeIngredient(name = "גרגרי תירס קראנצ'י (Cancha)", quantity = 30.0, unit = "גרם", costPerUnit = 0.015),
                    RecipeIngredient(name = "עלי כוסברה טרייה", quantity = 8.0, unit = "גרם", costPerUnit = 0.02)
                ),
                prepGuide = KitchenPrepGuide(
                    prepTimeMinutes = 8,
                    station = KitchenStation.SALAD,
                    steps = listOf(
                        "לחתוך את נתחי הדג לקוביות שוות של 2 ס\"מ, להניח בקערת נירוסטה מקוררת על קרח.",
                        "להמליח בנדיבות, להוסיף שום כתוש דק, פלפל צ'ילי לימו וכוסברה קצוצה.",
                        "לסחוט מיץ ליים רענן ולערבב בעדינות למשך 45 שניות ליצירת 'חלב נמר' (Leche de Tigre).",
                        "להוסיף את רצועות הבצל הסגול המקורר ולערבב פעם אחת בלבד.",
                        "להעביר מיד לצלחת קרמיקה קרה.",
                        "לקשט בפלחי בטטה מזוגגת בצד, תירס קלוי קראנצ'י מעל ועלה כוסברה."
                    ),
                    allergens = listOf("דגים"),
                    chefNotes = "אין להשרות את הדג בליים יותר מ-2 דקות כדי שלא יתבשל יתר על המידה ויאבד ממרקמו.",
                    platingGuide = "להגיש בקערת חרס קרה, למזוג את כל המיץ הלבן מסביב."
                )
            ),
            // Hamburguesa Artesanal
            103L to DishRecipe(
                itemId = 103L,
                itemName = "Hamburguesa Artesanal Doble Queso",
                categoryName = "Hamburguesas y Carnes",
                ingredients = listOf(
                    RecipeIngredient(name = "קציצת בקר אנגוס מיושן", quantity = 200.0, unit = "גרם", costPerUnit = 0.045),
                    RecipeIngredient(name = "לחמניית בריוש שומשום מוזהבת", quantity = 1.0, unit = "יח'", costPerUnit = 1.8),
                    RecipeIngredient(name = "פרוסות גבינת צ'דר אמיתית", quantity = 2.0, unit = "יח'", costPerUnit = 1.2),
                    RecipeIngredient(name = "בצל מקורמל בסוכר חום וסיידר", quantity = 45.0, unit = "גרם", costPerUnit = 0.015),
                    RecipeIngredient(name = "רוטב איולי מעושן הבית", quantity = 25.0, unit = "גרם", costPerUnit = 0.02),
                    RecipeIngredient(name = "חמוצים פרוסים בחומץ שמיר", quantity = 20.0, unit = "גרם", costPerUnit = 0.01)
                ),
                prepGuide = KitchenPrepGuide(
                    prepTimeMinutes = 10,
                    station = KitchenStation.GRILL,
                    steps = listOf(
                        "לקלות את לחמניית הבריוש על משטח חמאה עד להזהבה וקרמול.",
                        "להניח את קציצת האנגוס על פלנצ'ה לוהטת (220 מעלות), למעוך קלות ליצירת קראסט.",
                        "להפוך לאחר 3 דקות, להניח 2 פרוסות צ'דר ולכסות בפעמון נירוסטה עם מעט מים להמסה מלאה.",
                        "למרוח איולי מעושן על שני חלקי הלחמנייה.",
                        "להניח חמוצים בתחתית, מעליהם את הבורגר עם הצ'דר המותך.",
                        "לסיים בשכבת בצל מקורמל נדיבה ולסגור את הלחמנייה עם קיסם במבוק."
                    ),
                    allergens = listOf("גלוטן", "לקטוז", "ביצים"),
                    chefNotes = "מידת עשייה מומלצת: Medium-Well, זמן צלייה כולל 6 דקות.",
                    platingGuide = "מוגש על לוח עץ אלון עם קונוס צ'יפס רותח."
                )
            ),
            // Pizza Margherita
            104L to DishRecipe(
                itemId = 104L,
                itemName = "Pizza Margherita Rústica",
                categoryName = "Pizzas y Empanadas",
                ingredients = listOf(
                    RecipeIngredient(name = "כדור בצק מחמצת 72 שעות", quantity = 260.0, unit = "גרם", costPerUnit = 0.006),
                    RecipeIngredient(name = "רוטב עגבניות סאן מרזנו", quantity = 80.0, unit = "גרם", costPerUnit = 0.015),
                    RecipeIngredient(name = "מוצרלה די בופאלה טרייה", quantity = 110.0, unit = "גרם", costPerUnit = 0.04),
                    RecipeIngredient(name = "שמן זית כתית מעולה", quantity = 12.0, unit = "מ\"ל", costPerUnit = 0.025),
                    RecipeIngredient(name = "עלי בזיליקום טריים", quantity = 6.0, unit = "גרם", costPerUnit = 0.03)
                ),
                prepGuide = KitchenPrepGuide(
                    prepTimeMinutes = 7,
                    station = KitchenStation.OVEN,
                    steps = listOf(
                        "פתיחת כדור הבצק ידנית על קמח סמולינה, שמירה על שוליים תפוחים (Cornicione).",
                        "מריחה במצקת של רוטב עגבניות ממרכז הבצק כלפי חוץ בתנועה סיבובית.",
                        "פיזור קרעי מוצרלה טרייה באופן אחיד.",
                        "זילוף שמן זית עדין והכנסה לתנור אבן ב-380 מעלות למשך 3-4 דקות.",
                        "סיבוב הפיצה באמצע האפייה לקבלת השחמה שווה ונקודות נמר.",
                        "הוצאה, חיתוך ל-6 משולשים והנחת עלי בזיליקום טריים מעל."
                    ),
                    allergens = listOf("גלוטן", "לקטוז"),
                    chefNotes = "להוסיף את הבזיליקום רק לאחר היציאה מהתנור כדי למנוע את השחרתו.",
                    platingGuide = "מגש אבן עגול או קרטון פיצה ממותג."
                )
            ),
            // Tequenos
            105L to DishRecipe(
                itemId = 105L,
                itemName = "Tequeños de Queso con Guacamole",
                categoryName = "Entradas y Piqueos",
                ingredients = listOf(
                    RecipeIngredient(name = "אצבעות בצק וונטון פריך", quantity = 6.0, unit = "יח'", costPerUnit = 0.4),
                    RecipeIngredient(name = "גבינת קאסו בלנקו מלוחה", quantity = 120.0, unit = "גרם", costPerUnit = 0.03),
                    RecipeIngredient(name = "אבוקדו בשל לגוואקמולי", quantity = 80.0, unit = "גרם", costPerUnit = 0.02),
                    RecipeIngredient(name = "מיץ ליים, עגבניה וכוסברה", quantity = 25.0, unit = "גרם", costPerUnit = 0.02),
                    RecipeIngredient(name = "שמן טיגון עמוק", quantity = 30.0, unit = "מ\"ל", costPerUnit = 0.01)
                ),
                prepGuide = KitchenPrepGuide(
                    prepTimeMinutes = 6,
                    station = KitchenStation.WOK,
                    steps = listOf(
                        "חימום צ'יפסר טיגון עמוק ל-175 מעלות.",
                        "הורדת 6 יחידות טקניוס קפואים לרשת הטיגון למשך 3 דקות עד צבע זהוב עמוק.",
                        "סינון שמן על גבי נייר סופג והמלחה עדינה.",
                        "הכנת גוואקמולי טרי: מעיכת אבוקדו עם ליים, שום ועגבנייה קצוצה.",
                        "סידור היחידות בצורת כוכב סביב קערית הגוואקמולי."
                    ),
                    allergens = listOf("גלוטן", "לקטוז"),
                    chefNotes = "יש לוודא שהגבינה חמה ונמסה מבפנים והבצק פריך מאוד.",
                    platingGuide = "סלסילת מתכת או צלחת ארוכה מלבנית עם קערית רוטב במרכז."
                )
            ),
            // Chicha Morada
            106L to DishRecipe(
                itemId = 106L,
                itemName = "Chicha Morada Tradicional",
                categoryName = "Bebidas y Refrescos",
                ingredients = listOf(
                    RecipeIngredient(name = "תמצית תירס סגול מבושל עם תפוח וקינמון", quantity = 350.0, unit = "מ\"ל", costPerUnit = 0.005),
                    RecipeIngredient(name = "מיץ ליים טרי סחוט", quantity = 20.0, unit = "מ\"ל", costPerUnit = 0.02),
                    RecipeIngredient(name = "סירופ סוכר קנים", quantity = 25.0, unit = "מ\"ל", costPerUnit = 0.01),
                    RecipeIngredient(name = "קוביות קרח גבישי", quantity = 80.0, unit = "גרם", costPerUnit = 0.001),
                    RecipeIngredient(name = "פלח לימון ופירות חתוכים לקישוט", quantity = 1.0, unit = "יח'", costPerUnit = 0.3)
                ),
                prepGuide = KitchenPrepGuide(
                    prepTimeMinutes = 3,
                    station = KitchenStation.BAR,
                    steps = listOf(
                        "מילוי כוס זכוכית גבוהה בקוביות קרח.",
                        "מזיגת 350 מ\"ל צ'יצ'ה מוראדה קרה מחבית השירות.",
                        "הוספת 20 מ\"ל מיץ ליים רענן וסירופ סוכר.",
                        "ערבוב קל עם כפית בר ארוכה.",
                        "קישוט בפלח ליים טרי וקשית נייר מתכלה."
                    ),
                    allergens = emptyList(),
                    chefNotes = "טמפרטורת הגשה אידיאלית 3-5 מעלות, לא לדלל ביותר מדי קרח.",
                    platingGuide = "כוס הייבול שקופה על תחתית ממותגת."
                )
            )
        )
    }
}
