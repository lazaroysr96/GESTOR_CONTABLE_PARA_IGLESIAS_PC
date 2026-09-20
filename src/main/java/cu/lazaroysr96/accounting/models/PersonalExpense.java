package cu.lazaroysr96.accounting.models;

public class PersonalExpense {
    public String id;
    public String category;
    public String description;
    public double amount;
    public long createAt;

    public static final String[] CATEGORIES = {
        "Comida",
        "Transporte",
        "Salud",
        "Hogar",
        "Entretenimiento",
        "Educación",
        "Ropa",
        "Otros"
    };

    public static final String[] CATEGORY_EMOJIS = {
        "🍔",
        "🚌",
        "💊",
        "🏠",
        "🎮",
        "📚",
        "👕",
        "📦"
    };

    public static String getEmojiForCategory(String category) {
        for (int i = 0; i < CATEGORIES.length; i++) {
            if (CATEGORIES[i].equals(category)) {
                return CATEGORY_EMOJIS[i];
            }
        }
        return "📦";
    }
}
