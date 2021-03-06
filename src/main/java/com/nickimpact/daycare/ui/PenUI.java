package com.nickimpact.daycare.ui;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.nickimpact.daycare.DaycarePlugin;
import com.nickimpact.daycare.configuration.MsgConfigKeys;
import com.nickimpact.daycare.ranch.Pen;
import com.nickimpact.daycare.ranch.Pokemon;
import com.nickimpact.daycare.ranch.Ranch;
import com.nickimpact.daycare.stats.Statistics;
import com.nickimpact.daycare.ui.storage.PartyUI;
import com.nickimpact.daycare.utils.MessageUtils;
import com.nickimpact.impactor.gui.v2.Displayable;
import com.nickimpact.impactor.gui.v2.Icon;
import com.nickimpact.impactor.gui.v2.Layout;
import com.nickimpact.impactor.gui.v2.UI;
import com.pixelmonmod.pixelmon.storage.PixelmonStorage;
import com.pixelmonmod.pixelmon.storage.PlayerStorage;
import net.minecraft.entity.player.EntityPlayerMP;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.DyeColor;
import org.spongepowered.api.data.type.DyeColors;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.item.inventory.ClickInventoryEvent;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.property.InventoryDimension;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * (Some note will appear here)
 *
 * @author NickImpact (Nick DeGruccio)
 */
public class PenUI implements Displayable {

	/** The ranch being focused on */
	private Ranch ranch;

	/** The focus point for this UI */
	private Pen pen;

	private UI display;

	public PenUI(Player player, Ranch ranch, Pen pen, int id) {
		this.ranch = ranch;
		this.pen = pen;

		Map<String, Function<CommandSource, Optional<Text>>> tokens = Maps.newHashMap();
		tokens.put("pen_id", src -> Optional.of(Text.of(id)));
		this.display = UI.builder()
				.title(MessageUtils.fetchAndParseMsg(player, MsgConfigKeys.PEN_UI_TITLE, tokens, null))
				.dimension(InventoryDimension.of(9, 5))
				.build(player, DaycarePlugin.getInstance())
				.define(setupDisplay(player, id));
	}

	@Override
	public UI getDisplay() {
		return this.display;
	}

	private Layout setupDisplay(Player player, int id) {
		Layout.Builder builder = Layout.builder().dimension(InventoryDimension.of(9, 5));

		builder = this.drawBorder(player, builder);
		builder = this.drawPokemon(player, id, builder);
		builder = this.drawEgg(builder, player);

		Icon back = Icon.from(
				ItemStack.builder()
						.itemType(Sponge.getRegistry().getType(ItemType.class, "pixelmon:eject_button").orElse(ItemTypes.BARRIER))
						.add(Keys.DISPLAY_NAME, MessageUtils.fetchMsg(player, MsgConfigKeys.ITEM_BACK))
						.build()
		);
		back.addListener(clickable -> {
			this.close();
			new RanchUI(player).open();
		});
		builder.slot(back, 30);

		Icon purchase = Icon.from(
				ItemStack.builder()
						.itemType(ItemTypes.FILLED_MAP)
						.add(Keys.DISPLAY_NAME, MessageUtils.fetchMsg(MsgConfigKeys.PEN_UNLOCK))
						.build()
		);

		SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM d, yyyy");
		Map<String, Function<CommandSource, Optional<Text>>> tokens = Maps.newHashMap();
		tokens.put("date_unlocked", src -> Optional.of(Text.of(sdf.format(this.pen.getDateUnlocked()))));
		tokens.put("price", src -> {
			if(this.pen.getPrice().signum() == -1) {
				return Optional.of(Text.of("Free!"));
			}

			return Optional.of(DaycarePlugin.getInstance().getEconomy().getDefaultCurrency().format(this.pen.getPrice()));
		});
		purchase.getDisplay().offer(Keys.ITEM_LORE, Lists.newArrayList(
				Text.of()
		));

		return builder.build();
	}

	private Layout.Builder drawPokemon(Player player, int penID, Layout.Builder builder) {
		Optional<Pokemon> slot1 = this.pen.getAtPosition(1);
		Optional<Pokemon> slot2 = this.pen.getAtPosition(2);

		Icon s1 = slot1.map(pokemon -> StandardIcons.getPicture(player, pokemon, DaycarePlugin.getInstance().getMsgConfig().get(MsgConfigKeys.POKEMON_LORE_PEN)))
				.orElseGet(() -> Icon.from(ItemStack.builder().itemType(ItemTypes.BARRIER).add(Keys.DISPLAY_NAME, MessageUtils.fetchMsg(MsgConfigKeys.PEN_EMPTY_SLOT)).build()));

		Icon s2 = slot2.map(pokemon -> StandardIcons.getPicture(player, pokemon, DaycarePlugin.getInstance().getMsgConfig().get(MsgConfigKeys.POKEMON_LORE_PEN)))
				.orElseGet(() -> Icon.from(ItemStack.builder().itemType(ItemTypes.BARRIER).add(Keys.DISPLAY_NAME, MessageUtils.fetchMsg(MsgConfigKeys.PEN_EMPTY_SLOT)).build()));

		s1.addListener(clickable -> {
			clickable.getPlayer().closeInventory();
			if(slot1.isPresent()) {
				new SelectionUI(clickable.getPlayer(), ranch, pen, penID, slot1.get(), 1).open();
			} else {
				new PartyUI(clickable.getPlayer(), this.ranch, this.pen, penID, 1).open();
			}
		});

		s2.addListener(clickable -> {
			clickable.getPlayer().closeInventory();
			if(slot2.isPresent()) {
				new SelectionUI(clickable.getPlayer(), ranch, pen, penID, slot2.get(), 2).open();
			} else {
				new PartyUI(clickable.getPlayer(), this.ranch, this.pen, penID, 2).open();
			}
		});

		return builder.slot(s1, 11).slot(s2, 15);
	}

	private Layout.Builder drawEgg(Layout.Builder builder, Player player) {
		if(this.pen.isFull()) {
			Optional<Pokemon> optEgg = this.pen.getEgg();

			Icon icon = optEgg.map(egg -> StandardIcons.getPicture(player, egg, DaycarePlugin.getInstance().getMsgConfig().get(MsgConfigKeys.POKEMON_LORE_PEN)))
					.orElseGet(() -> Icon.from(ItemStack.builder().itemType(ItemTypes.BARRIER).add(Keys.DISPLAY_NAME, MessageUtils.fetchMsg(player, MsgConfigKeys.PEN_NO_EGG)).build()));

			optEgg.ifPresent(egg -> {
				icon.getDisplay().offer(Keys.ITEM_LORE, MessageUtils.fetchMsgs(player, MsgConfigKeys.PEN_EGG_PRESENT));
				icon.addListener(clickable -> {
					if(clickable.getEvent() instanceof ClickInventoryEvent.Primary) {
						Optional<PlayerStorage> optStor = PixelmonStorage.pokeBallManager.getPlayerStorage((EntityPlayerMP) clickable.getPlayer());
						optStor.ifPresent(storage -> {
							storage.addToParty(egg.getPokemon());
							storage.sendUpdatedList();
							this.ranch.getStats().incrementStat(Statistics.Stats.EGGS_COLLECTED);
							clickable.getPlayer().sendMessages(MessageUtils.fetchMsgs(player, MsgConfigKeys.PEN_EGG_CLAIM));
						});
					} else {
						this.ranch.getStats().incrementStat(Statistics.Stats.EGGS_DELETED);
						clickable.getPlayer().sendMessages(MessageUtils.fetchMsgs(player, MsgConfigKeys.PEN_EGG_DISMISSED));
					}
					this.pen.setEgg(null);
					getDisplay().setSlot(13, Icon.from(ItemStack.builder().itemType(ItemTypes.BARRIER).add(Keys.DISPLAY_NAME, Text.of(TextColors.RED, "No Egg Available...")).build()));
					this.drawBorder(player);
				});
			});

			builder = builder.slot(icon, 13);
		}
		return builder;
	}

	private void drawBorder(Player player) {
		Icon icon = Icon.from(ItemStack.builder().itemType(ItemTypes.STAINED_GLASS_PANE).add(Keys.DISPLAY_NAME, getTitle(player)).add(Keys.DYE_COLOR, getColoring()).build());
		int i;
		for(i = 0; i < 9; ++i) {
			this.display.setSlot(i, icon);
			this.display.setSlot(this.display.getDimension().getRows() * this.display.getDimension().getColumns() - i - 1, icon);
		}

		for(i = 1; i < this.display.getDimension().getRows() - 1; ++i) {
			this.display.setSlot(i * 9, icon);
			this.display.setSlot((i + 1) * 9 - 1, icon);
		}

		for(i = 19; i < 26; i++) {
			this.display.setSlot(i, icon);
		}
	}

	/**
	 * Initial call for border drawing. This method will apply the changes to the layout builder it is given, rather than
	 * attempt to set the slots directly.
	 *
	 * @param builder The current instance of the layout builder
	 */
	private Layout.Builder drawBorder(Player player, Layout.Builder builder) {
		ItemStack pane = ItemStack.builder().itemType(ItemTypes.STAINED_GLASS_PANE).add(Keys.DISPLAY_NAME, getTitle(player)).add(Keys.DYE_COLOR, getColoring()).build();
		return builder.border(Icon.from(pane)).slots(Icon.from(pane), 19, 20, 21, 22, 23, 24, 25);
	}

	private Text getTitle(Player player) {
		Text title = Text.EMPTY;
		if(pen.isFull()) {
			if(this.pen.canBreed()) {
				title = MessageUtils.fetchMsg(player, MsgConfigKeys.PEN_TITLES_BREEDING);
			} else if(this.pen.getEgg().isPresent()) {
				title = MessageUtils.fetchMsg(player, MsgConfigKeys.PEN_TITLES_EGG_AVAILABLE);
			} else {
				title = MessageUtils.fetchMsg(player, MsgConfigKeys.PEN_TITLES_UNABLE);
			}
		}
		return title;
	}

	private DyeColor getColoring() {
		DyeColor coloring = DyeColors.BLACK;
		if(pen.isFull()) {
			if(this.pen.canBreed()) {
				coloring = DyeColors.GREEN;
			} else if(this.pen.getEgg().isPresent()) {
				coloring = DyeColors.YELLOW;
			} else {
				coloring = DyeColors.RED;
			}
		}
		return coloring;
	}
}
