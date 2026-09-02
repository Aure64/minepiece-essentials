package com.minepiece.essentials.donate;

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

    private static final int[] PRESETS = {100_000, 500_000, 1_000_000};

    private enum State { AMOUNT, CONFIRM }

    private final Screen parent;
    private State state = State.AMOUNT;

    // Champ de saisie du montant (état AMOUNT).
    private TextFieldWidget amountField;
    private String pendingText = "";
    private String errorMessage = null;

    // Montant validé, en attente de confirmation (état CONFIRM).
    private long confirmedAmount;

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
        clearAndInit();
    }

    private void initConfirmState() {
        int centerX = width / 2;
        int y = height / 2;

        addDrawableChild(ButtonWidget.builder(Text.translatable("minepiece.ui.donate.btn_confirm"),
                b -> sendDonation()).dimensions(centerX - 100, y + 20, 96, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("minepiece.ui.donate.btn_back"),
                b -> {
                    state = State.AMOUNT;
                    clearAndInit();
                }).dimensions(centerX + 4, y + 20, 96, 20).build());
    }

    private void sendDonation() {
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

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);
        super.render(ctx, mouseX, mouseY, delta);

        int centerX = width / 2;
        if (state == State.AMOUNT) {
            ctx.drawCenteredTextWithShadow(textRenderer, title, centerX, height / 2 - 70, 0xFFFFFFFF);
            if (errorMessage != null) {
                ctx.drawCenteredTextWithShadow(textRenderer, errorMessage, centerX, height / 2 - 20, 0xFFFF5555);
            }
        } else {
            ctx.drawCenteredTextWithShadow(textRenderer, title, centerX, height / 2 - 40, 0xFFFFFFFF);
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
