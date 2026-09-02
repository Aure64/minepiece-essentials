package com.minepiece.essentials.donate;

import com.minepiece.essentials.ServerDetector;
import com.minepiece.essentials.hud.ParchmentRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/**
 * Petit écran de don : saisie d'un montant en berries, puis confirmation
 * explicite affichant la commande exacte avant envoi. Action strictement
 * initiée par un clic humain : pas de rejeu, pas de mémorisation des dons
 * précédents.
 *
 * Construit uniquement avec des widgets vanilla (ButtonWidget / TextFieldWidget)
 * et sans override de mouseClicked, pour rester identique entre les portages
 * 1.21.11 et 1.21.8 (signature de mouseClicked différente entre les deux).
 */
public class DonateScreen extends Screen {

    /** Nom du joueur receveur des dons in-game (auteur du mod). */
    private static final String RECIPIENT = "Aure64";

    private static final int[] PRESETS = {50_000, 100_000, 250_000};

    // Géométrie de l'état AMOUNT (voir initAmountState) : sert de référence pour
    // calculer, sans jamais recouvrir, la position des boutons de l'état CONFIRM.
    private static final int AMOUNT_FIRST_ROW_Y = -50; // relatif à height/2
    private static final int AMOUNT_ROW_GAP_1 = 28;
    private static final int AMOUNT_ROW_GAP_2 = 32;
    private static final int BUTTON_HEIGHT = 20;
    private static final int AMOUNT_BUTTONS_Y =
            AMOUNT_FIRST_ROW_Y + AMOUNT_ROW_GAP_1 + AMOUNT_ROW_GAP_2; // = height/2 - 50 + 28 + 32 = height/2 + 10
    private static final int AMOUNT_BUTTONS_BOTTOM = AMOUNT_BUTTONS_Y + BUTTON_HEIGHT; // = height/2 + 30

    // Marge de sécurité entre le bas des boutons AMOUNT et le haut des boutons
    // CONFIRM : garantit qu'un double-clic rapide sur "Suivant" ne peut jamais
    // faire tomber le second clic sur "Confirmer" (voir aussi CONFIRM_ARM_DELAY_MS
    // ci-dessous, seconde défense indépendante).
    private static final int CONFIRM_SAFETY_MARGIN = 20;
    private static final int CONFIRM_BUTTONS_Y = AMOUNT_BUTTONS_BOTTOM + CONFIRM_SAFETY_MARGIN; // = height/2 + 50

    // Délai minimal (ms) entre l'entrée dans l'état CONFIRM et la prise en compte
    // d'un clic sur "Confirmer". Défense indépendante de la géométrie : même si
    // un futur changement de layout réintroduisait un chevauchement, un double-clic
    // en rafale (les deux clics arrivent dans la même frame ou la suivante) ne
    // pourrait plus déclencher un envoi.
    static final long CONFIRM_ARM_DELAY_MS = 300;

    private enum State { AMOUNT, CONFIRM }

    private final Screen parent;
    private State state = State.AMOUNT;

    // Champ de saisie du montant (état AMOUNT).
    private TextFieldWidget amountField;
    private String pendingText = "";
    private String errorMessage = null;

    // Montant validé, en attente de confirmation (état CONFIRM).
    private long confirmedAmount;

    // Horodatage d'entrée dans l'état CONFIRM ; voir CONFIRM_ARM_DELAY_MS.
    private long confirmEnteredAt = 0;

    public DonateScreen(Screen parent) {
        super(Text.translatable("minepiece.ui.donate.title"));
        this.parent = parent;
    }

    /**
     * Parse un montant saisi par l'utilisateur : chiffres uniquement, les espaces
     * étant tolérés comme séparateurs de milliers. Doit être strictement positif
     * et tenir dans un {@code long}. Lève {@link IllegalArgumentException} pour
     * tout texte invalide (vide, lettres, zéro, négatif...).
     */
    public static long parseAmount(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("empty");
        }
        String cleaned = raw.replace(" ", "").trim();
        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("empty");
        }
        if (!cleaned.chars().allMatch(Character::isDigit)) {
            throw new IllegalArgumentException("not digits");
        }
        long value;
        try {
            value = Long.parseLong(cleaned);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("too large", e);
        }
        if (value <= 0) {
            throw new IllegalArgumentException("not positive");
        }
        return value;
    }

    @Override
    protected void init() {
        if (state == State.AMOUNT) {
            initAmountState();
        } else {
            initConfirmState();
        }
    }

    private void initAmountState() {
        int centerX = width / 2;
        int y = height / 2 - 50;

        amountField = new TextFieldWidget(textRenderer, centerX - 100, y, 200, 20,
                Text.translatable("minepiece.ui.donate.field_amount"));
        amountField.setMaxLength(32);
        amountField.setText(pendingText);
        amountField.setChangedListener(text -> pendingText = text);
        addDrawableChild(amountField);
        setInitialFocus(amountField);

        y += 28;
        int presetW = 64;
        int gap = 4;
        int presetsX = centerX - (presetW * 3 + gap * 2) / 2;
        for (int i = 0; i < PRESETS.length; i++) {
            int amount = PRESETS[i];
            addDrawableChild(ButtonWidget.builder(Text.literal(formatBerries(amount)),
                    b -> {
                        pendingText = Integer.toString(amount);
                        amountField.setText(pendingText);
                    }).dimensions(presetsX + i * (presetW + gap), y, presetW, 20).build());
        }

        y += 32;
        addDrawableChild(ButtonWidget.builder(Text.translatable("minepiece.ui.donate.btn_next"),
                b -> validateAndAdvance()).dimensions(centerX - 100, y, 96, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("minepiece.ui.donate.btn_cancel"),
                b -> close()).dimensions(centerX + 4, y, 96, 20).build());
    }

    private void validateAndAdvance() {
        try {
            confirmedAmount = parseAmount(pendingText);
        } catch (IllegalArgumentException e) {
            errorMessage = Text.translatable("minepiece.ui.donate.error_invalid").getString();
            return;
        }
        errorMessage = null;
        state = State.CONFIRM;
        confirmEnteredAt = System.currentTimeMillis();
        clearAndInit();
    }

    private void initConfirmState() {
        int centerX = width / 2;

        addDrawableChild(ButtonWidget.builder(Text.translatable("minepiece.ui.donate.btn_confirm"),
                b -> sendDonation()).dimensions(centerX - 100, height / 2 + CONFIRM_BUTTONS_Y, 96, BUTTON_HEIGHT).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("minepiece.ui.donate.btn_back"),
                b -> {
                    state = State.AMOUNT;
                    clearAndInit();
                }).dimensions(centerX + 4, height / 2 + CONFIRM_BUTTONS_Y, 96, BUTTON_HEIGHT).build());
    }

    /**
     * Vrai si le délai d'armement est écoulé depuis l'entrée en état CONFIRM.
     * Extrait en méthode statique pure pour être testable sans écran réel :
     * seconde défense contre un envoi accidentel (voir CONFIRM_ARM_DELAY_MS),
     * indépendante de la géométrie des boutons.
     */
    static boolean isArmed(long enteredAt, long now) {
        return now - enteredAt >= CONFIRM_ARM_DELAY_MS;
    }

    private void sendDonation() {
        if (!ServerDetector.isOnMinePiece()) {
            // Ecran ouvert sur MinePiece mais qui aurait survécu à un changement
            // de contexte (autre serveur) : on n'envoie rien.
            close();
            return;
        }
        if (!isArmed(confirmEnteredAt, System.currentTimeMillis())) {
            // Clic trop rapproché de l'entrée en état CONFIRM (ex. double-clic
            // sur "Suivant") : on ignore, la confirmation n'a pas pu être lue.
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            close();
            return;
        }
        // Un seul envoi, déclenché par ce clic de confirmation. Pas de slash
        // en tête : c'est le pattern maison de sendChatCommand.
        client.player.networkHandler.sendChatCommand("pay " + RECIPIENT + " " + confirmedAmount);
        close();
    }

    private String commandPreview() {
        return "/pay " + RECIPIENT + " " + confirmedAmount;
    }

    private static String formatBerries(int amount) {
        return String.format("%,d", amount).replace(',', ' ');
    }

    /**
     * Fond plat façon HelpScreen, sans flou vanilla. Screen.render() appelle déjà
     * renderBackground() une fois pour dessiner l'arrière-plan avant les widgets ;
     * on ne doit donc PAS l'appeler nous-même en plus dans render(), sinon le flou
     * vanilla (Screen$BlurredBackgroundRenderer) est déclenché deux fois dans la
     * même frame et Minecraft lève "Can only blur once per frame".
     */
    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, 0xB0000000);
        int[] panel = panelBounds();
        ParchmentRenderer.renderPanel(ctx, panel[0], panel[1], panel[2], panel[3], null);
    }

    /** Zone du panneau parchemin derrière les widgets, selon l'état courant. */
    private int[] panelBounds() {
        int centerX = width / 2;
        if (state == State.AMOUNT) {
            int y = height / 2 - 96;
            return new int[]{centerX - 130, y, 260, 146};
        } else {
            int y = height / 2 - 56;
            // Hauteur agrandie pour couvrir les boutons CONFIRM, désormais plus bas
            // (CONFIRM_BUTTONS_Y) qu'avant le correctif anti-chevauchement.
            int bottom = height / 2 + CONFIRM_BUTTONS_Y + BUTTON_HEIGHT + 10;
            return new int[]{centerX - 130, y, 260, bottom - y};
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);

        int centerX = width / 2;
        if (state == State.AMOUNT) {
            ctx.drawCenteredTextWithShadow(textRenderer, title, centerX, height / 2 - 70, 0xFFF0A857);
            if (errorMessage != null) {
                ctx.drawCenteredTextWithShadow(textRenderer, errorMessage, centerX, height / 2 - 20, 0xFFFF5555);
            }
        } else {
            ctx.drawCenteredTextWithShadow(textRenderer, title, centerX, height / 2 - 40, 0xFFF0A857);
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.translatable("minepiece.ui.donate.confirm_prompt").getString(),
                    centerX, height / 2 - 20, 0xFFFFE9D5);
            ctx.drawCenteredTextWithShadow(textRenderer, commandPreview(), centerX, height / 2, 0xFFFFD27F);
        }
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
