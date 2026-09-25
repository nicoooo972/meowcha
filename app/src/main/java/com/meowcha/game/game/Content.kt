package com.meowcha.game.game

import androidx.compose.ui.graphics.Color

enum class IngredientKind { LIQUID, TOPPING }

enum class Ingredient(
    val label: String,
    val emoji: String,
    val color: Color,
    val kind: IngredientKind,
    val unlockDay: Int,
) {
    ESPRESSO("Espresso", "☕", Color(0xFF6D4C41), IngredientKind.LIQUID, 1),
    WATER("Eau chaude", "💧", Color(0xFFB3E5FC), IngredientKind.LIQUID, 1),
    MILK("Lait", "🥛", Color(0xFFFFF8E1), IngredientKind.LIQUID, 1),
    FOAM("Mousse", "☁️", Color(0xFFFFFDF7), IngredientKind.TOPPING, 1),
    STRAWBERRY("Sirop fraise", "🍓", Color(0xFFF06292), IngredientKind.LIQUID, 2),
    MATCHA("Matcha", "🍵", Color(0xFF8BC34A), IngredientKind.LIQUID, 2),
    CHOCO("Chocolat", "🍫", Color(0xFF5D4037), IngredientKind.LIQUID, 3),
    ICE("Glaçons", "🧊", Color(0xFFE1F5FE), IngredientKind.LIQUID, 3),
    CREAM("Chantilly", "🍦", Color(0xFFFFFFFF), IngredientKind.TOPPING, 3),
    VANILLA("Sirop vanille", "🌼", Color(0xFFFFE082), IngredientKind.LIQUID, 4),
    CARAMEL("Caramel", "🍯", Color(0xFFD9892B), IngredientKind.TOPPING, 4),
    MARSHMALLOW("Guimauves", "🍡", Color(0xFFF8BBD0), IngredientKind.TOPPING, 4),
    TEA("Thé noir", "🫖", Color(0xFFA1673B), IngredientKind.LIQUID, 5),
    SAKURA("Pétales sakura", "🌸", Color(0xFFFF80AB), IngredientKind.TOPPING, 5),
}

data class Recipe(
    val id: String,
    val name: String,
    val steps: List<Ingredient>,
    val price: Int,
    val unlockDay: Int,
)

object Recipes {
    private val base = listOf(
        Recipe("espresso", "Espresso Minou", listOf(Ingredient.ESPRESSO), 3, 1),
        Recipe("americano", "Americano", listOf(Ingredient.ESPRESSO, Ingredient.WATER), 4, 1),
        Recipe("latte", "Latte Câlin", listOf(Ingredient.ESPRESSO, Ingredient.MILK, Ingredient.FOAM), 5, 1),
        Recipe("cappuccino", "Cappuccino Nuage", listOf(Ingredient.ESPRESSO, Ingredient.FOAM, Ingredient.FOAM), 5, 1),
        Recipe("pinklatte", "Pink Latte", listOf(Ingredient.ESPRESSO, Ingredient.STRAWBERRY, Ingredient.MILK, Ingredient.FOAM), 7, 2),
        Recipe("matchalatte", "Matcha Latte", listOf(Ingredient.MATCHA, Ingredient.MILK, Ingredient.FOAM), 6, 2),
        Recipe("strawmilk", "Lait Fraise", listOf(Ingredient.STRAWBERRY, Ingredient.MILK, Ingredient.MILK), 5, 2),
        Recipe("mocha", "Moka Moustache", listOf(Ingredient.ESPRESSO, Ingredient.CHOCO, Ingredient.MILK, Ingredient.CREAM), 7, 3),
        Recipe("icedlatte", "Latte Glacé", listOf(Ingredient.ICE, Ingredient.ESPRESSO, Ingredient.MILK), 6, 3),
        Recipe("hotchoc", "Chocolat Chaton", listOf(Ingredient.CHOCO, Ingredient.MILK, Ingredient.CREAM, Ingredient.MARSHMALLOW), 8, 4),
        Recipe("caramelmac", "Caramel Macchiato", listOf(Ingredient.VANILLA, Ingredient.MILK, Ingredient.ESPRESSO, Ingredient.CARAMEL), 8, 4),
        Recipe("teafog", "Thé Rose Brumeux", listOf(Ingredient.TEA, Ingredient.VANILLA, Ingredient.MILK, Ingredient.FOAM), 7, 5),
        Recipe("sakuramatcha", "Sakura Matcha", listOf(Ingredient.MATCHA, Ingredient.STRAWBERRY, Ingredient.MILK, Ingredient.SAKURA), 9, 5),
        Recipe("special", "Meowcha Spécial", listOf(Ingredient.MATCHA, Ingredient.ESPRESSO, Ingredient.MILK, Ingredient.CREAM, Ingredient.SAKURA), 12, 6),
    )

    /** Recettes de base + celles des packs de contenu téléchargés. */
    @Volatile var all: List<Recipe> = base
        private set

    fun setExtra(extra: List<Recipe>) {
        all = base + extra.filter { e -> base.none { it.id == e.id } }
    }

    fun available(day: Int) = all.filter { it.unlockDay <= day }
}

enum class FurPattern { PLAIN, TABBY, CALICO, TUXEDO, POINTS }
enum class Accessory { NONE, BOW, FLOWER, GLASSES, BELL, CROWN }

data class CatCustomer(
    val id: String,
    val name: String,
    val fur: Color,
    val accent: Color,
    val eyes: Color,
    val pattern: FurPattern,
    val accessory: Accessory,
    val accessoryColor: Color,
    val favorite: String,
    val quote: String,
)

object Cats {
    private val base = listOf(
        CatCustomer("mochi", "Mochi", Color(0xFFFFFFFF), Color(0xFFF8BBD0), Color(0xFF4FC3F7), FurPattern.PLAIN, Accessory.BOW, Color(0xFFFF4081), "pinklatte", "Un latte tout rose, s'il te plaît ~"),
        CatCustomer("caramel", "Caramel", Color(0xFFFFB74D), Color(0xFFE65100), Color(0xFF8BC34A), FurPattern.TABBY, Accessory.NONE, Color.Transparent, "caramelmac", "Miaou ! J'ai besoin de sucre !"),
        CatCustomer("sakura", "Sakura", Color(0xFFFFF3E0), Color(0xFFFF8A65), Color(0xFFFFB300), FurPattern.CALICO, Accessory.FLOWER, Color(0xFFFF80AB), "sakuramatcha", "Les fleurs sont jolies aujourd'hui !"),
        CatCustomer("oreo", "Oréo", Color(0xFF424242), Color(0xFFFFFFFF), Color(0xFFFFEB3B), FurPattern.TUXEDO, Accessory.BELL, Color(0xFFFFD54F), "mocha", "Bonjour, madame la barista."),
        CatCustomer("luna", "Luna", Color(0xFFB0BEC5), Color(0xFF78909C), Color(0xFFBA68C8), FurPattern.PLAIN, Accessory.GLASSES, Color(0xFFAD1457), "teafog", "Je lisais un livre... un thé ?"),
        CatCustomer("praline", "Praline", Color(0xFFA1887F), Color(0xFF6D4C41), Color(0xFF81C784), FurPattern.TABBY, Accessory.BOW, Color(0xFFCE93D8), "hotchoc", "Du chocolat, beaucoup de chocolat !"),
        CatCustomer("nuage", "Nuage", Color(0xFFECEFF1), Color(0xFFCFD8DC), Color(0xFF64B5F6), FurPattern.POINTS, Accessory.CROWN, Color(0xFFFFD54F), "cappuccino", "Je suis la princesse des nuages."),
        CatCustomer("peche", "Pêche", Color(0xFFFFCCBC), Color(0xFFFF8A65), Color(0xFF26A69A), FurPattern.PLAIN, Accessory.FLOWER, Color(0xFFFFF176), "strawmilk", "Coucou ! Tu es trop mignonne !"),
        CatCustomer("reglisse", "Réglisse", Color(0xFF212121), Color(0xFF424242), Color(0xFFFFC107), FurPattern.PLAIN, Accessory.BOW, Color(0xFFF06292), "espresso", "...Espresso. Vite."),
        CatCustomer("biscotte", "Biscotte", Color(0xFFFFF8E1), Color(0xFF795548), Color(0xFF42A5F5), FurPattern.POINTS, Accessory.NONE, Color.Transparent, "latte", "Un latte bien chaud, merci !"),
        CatCustomer("guimauve", "Guimauve", Color(0xFFFCE4EC), Color(0xFFF48FB1), Color(0xFFEC407A), FurPattern.CALICO, Accessory.CROWN, Color(0xFFF8BBD0), "special", "On m'a parlé d'une boisson secrète..."),
        CatCustomer("tigrou", "Tigrou", Color(0xFFFFCC80), Color(0xFF8D6E63), Color(0xFF66BB6A), FurPattern.TABBY, Accessory.BELL, Color(0xFFE57373), "icedlatte", "Il fait trop chaud, un truc glacé !"),
    )

    /** Chats de base + ceux des packs de contenu téléchargés. */
    @Volatile var all: List<CatCustomer> = base
        private set

    fun setExtra(extra: List<CatCustomer>) {
        all = base + extra.filter { e -> base.none { it.id == e.id } }
    }

    fun byId(id: String) = all.firstOrNull { it.id == id } ?: base.first()
}

enum class MugPattern { NONE, DOTS, HEARTS, STRIPES, PAWS, STRAWBERRIES, FLOWERS }

data class Mug(
    val id: String,
    val name: String,
    val body: Color,
    val detail: Color,
    val pattern: MugPattern,
    val catEars: Boolean,
    val price: Int,
    /** Bonus de pourboire en pourcentage. */
    val tipBonus: Int,
    /** Chemin vers un modèle .glb (dans assets/) pour un rendu 3D à la place du dessin Canvas. */
    val model3d: String? = null,
)

object Mugs {
    val all = listOf(
        Mug("classic", "Classique crème", Color(0xFFFFFBF5), Color(0xFFF8BBD0), MugPattern.NONE, false, 0, 0),
        Mug("bonbon", "Rose bonbon", Color(0xFFF8BBD0), Color(0xFFFFFFFF), MugPattern.NONE, false, 25, 5),
        Mug("dots", "Petits pois", Color(0xFFFFFFFF), Color(0xFFF06292), MugPattern.DOTS, false, 45, 8),
        Mug("stripes", "Marinière rose", Color(0xFFFFFFFF), Color(0xFFF48FB1), MugPattern.STRIPES, false, 60, 10),
        Mug("hearts", "Plein de cœurs", Color(0xFFFFEBEE), Color(0xFFE91E63), MugPattern.HEARTS, false, 80, 12),
        Mug("lavender", "Lavande pattes", Color(0xFFE1BEE7), Color(0xFF8E24AA), MugPattern.PAWS, false, 100, 15),
        Mug("strawberry", "Fraisier", Color(0xFFFFF0F3), Color(0xFFE53935), MugPattern.STRAWBERRIES, false, 130, 18),
        Mug("kitty", "Oreilles de chat", Color(0xFFFFFFFF), Color(0xFFF48FB1), MugPattern.NONE, true, 160, 22),
        Mug("sakura", "Sakura royal", Color(0xFFFCE4EC), Color(0xFFFF80AB), MugPattern.FLOWERS, true, 250, 30),
        // Tasses en modèle 3D (premier export Blender, spike 2.0.0)
        Mug("classic3d", "Classique 3D", Color(0xFFFFFBF5), Color(0xFF8C593A), MugPattern.NONE, false, 300, 25, "models/mug_classic.glb"),
        Mug("matcha3d", "Matcha 3D", Color(0xFF9EC78C), Color(0xFF73994D), MugPattern.NONE, false, 320, 28, "models/mug_matcha.glb"),
        Mug("sakura3d", "Sakura 3D", Color(0xFFFAB8CC), Color(0xFF8C593A), MugPattern.NONE, true, 340, 30, "models/mug_sakura.glb"),
        Mug("minuit3d", "Minuit 3D", Color(0xFF333A61), Color(0xFF4D3323), MugPattern.NONE, true, 360, 32, "models/mug_minuit.glb"),
    )

    fun byId(id: String) = all.firstOrNull { it.id == id } ?: all.first()
}

enum class DecorBonus { PATIENCE, TIPS, VIP }

/** Décorations du café : visibles dans le décor et donnent un petit bonus. */
data class Decor(
    val id: String,
    val name: String,
    val emoji: String,
    val price: Int,
    val bonus: DecorBonus,
    val amount: Int,
    val description: String,
)

object Decors {
    val all = listOf(
        Decor("plant", "Plante en pot", "🪴", 40, DecorBonus.PATIENCE, 5, "Les chats sont plus zen : +5% de patience"),
        Decor("garland", "Guirlande lumineuse", "✨", 70, DecorBonus.TIPS, 5, "Ambiance cosy : +5% de pourboires"),
        Decor("rug", "Tapis rose moelleux", "🧶", 90, DecorBonus.PATIENCE, 8, "Doux sous les pattes : +8% de patience"),
        Decor("painting", "Portrait de chat", "🖼️", 120, DecorBonus.TIPS, 8, "Très chic : +8% de pourboires"),
        Decor("cattree", "Arbre à chat", "🐈", 180, DecorBonus.VIP, 10, "Attire les chats VIP : +10% de chance"),
        Decor("lamp", "Lampe nuage", "☁️", 220, DecorBonus.PATIENCE, 12, "Lumière douce : +12% de patience"),
        Decor("piano", "Petit piano rose", "🎹", 350, DecorBonus.TIPS, 15, "Musique douce : +15% de pourboires"),
    )

    fun bonus(owned: Set<String>, type: DecorBonus) =
        all.filter { it.id in owned && it.bonus == type }.sumOf { it.amount }
}

enum class ObjectiveType { PERFECT, COMBO, VIP, NO_LEAVE, COINS, PETS }

/** Objectif du jour, avec sa récompense en pièces. */
data class Objective(val type: ObjectiveType, val target: Int, val reward: Int) {
    val label: String
        get() = when (type) {
            ObjectiveType.PERFECT -> "Servir $target boissons parfaites"
            ObjectiveType.COMBO -> "Atteindre un combo x$target"
            ObjectiveType.VIP -> "Servir parfaitement $target chat VIP"
            ObjectiveType.NO_LEAVE -> "Aucun chat ne part fâché"
            ObjectiveType.COINS -> "Gagner $target pièces"
            ObjectiveType.PETS -> "Caresser $target chats"
        }
    val emoji: String
        get() = when (type) {
            ObjectiveType.PERFECT -> "⭐"
            ObjectiveType.COMBO -> "🔥"
            ObjectiveType.VIP -> "👑"
            ObjectiveType.NO_LEAVE -> "💗"
            ObjectiveType.COINS -> "🪙"
            ObjectiveType.PETS -> "🐾"
        }

    companion object {
        fun forDay(day: Int, customers: Int): List<Objective> {
            val pool = listOf(
                Objective(ObjectiveType.PERFECT, (customers * 0.6f).toInt().coerceAtLeast(2), 10 + day * 2),
                Objective(ObjectiveType.COMBO, (2 + day / 2).coerceAtMost(6), 12 + day * 2),
                Objective(ObjectiveType.VIP, 1, 15 + day),
                Objective(ObjectiveType.NO_LEAVE, 1, 10 + day * 2),
                Objective(ObjectiveType.COINS, customers * 5, 8 + day * 2),
                Objective(ObjectiveType.PETS, (customers / 2).coerceAtLeast(2), 8 + day),
            )
            return pool.shuffled(kotlin.random.Random(day * 7919L)).take(3)
        }
    }
}
