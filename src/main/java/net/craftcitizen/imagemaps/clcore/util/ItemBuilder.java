package net.craftcitizen.imagemaps.clcore.util;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import net.md_5.bungee.api.ChatColor;

public class ItemBuilder {
    private final ItemStack item;
    private final ItemMeta meta;

    public ItemBuilder(@Nonnull final ItemStack item) {
        this.item = item.clone();
        this.meta = item.getItemMeta();
    }

    public ItemBuilder(@Nonnull final Material material) { this(new ItemStack(material)); }

    public ItemStack build() {
        this.item.setItemMeta(this.meta);
        return this.item;
    }

    @SuppressWarnings("deprecation")
    public ItemBuilder setDisplayName(@Nullable final String name) {
        this.meta.setDisplayName(name == null ? null : ChatColor.translateAlternateColorCodes((char) '&', (String) name));
        return this;
    }

    @SuppressWarnings("deprecation")
    public ItemBuilder setType(@Nonnull final Material material) {
        this.item.setType(material);
        return this;
    }

    public ItemBuilder setAmount(final int amount) {
        if (amount < 1 || amount > this.item.getMaxStackSize()) {
            throw new IllegalArgumentException(
                    "You must specify a value larger than 0 and less than " + this.item.getMaxStackSize() + " (inclusive)");
        }
        this.item.setAmount(amount);
        return this;
    }

    public ItemBuilder setLore(final String... lore) {
        this.setLore(Arrays.asList(lore));
        return this;
    }

    @SuppressWarnings("deprecation")
    public ItemBuilder setLore(final List<String> lore) {
        this.meta.setLore(
                lore.stream().map(line -> ChatColor.translateAlternateColorCodes((char) '&', (String) line)).collect(Collectors.toList()));
        return this;
    }

    public ItemBuilder setCustomModelData(final int customModelData) {
        this.meta.setCustomModelData(Integer.valueOf(customModelData));
        return this;
    }

    public ItemBuilder setUnbreakable(final boolean isUnbreakable) {
        this.meta.setUnbreakable(isUnbreakable);
        return this;
    }

    public ItemBuilder addEnchant(@Nonnull final Enchantment enchantment, final int level, final boolean allowUnsafe) {
        this.meta.addEnchant(enchantment, level, allowUnsafe);
        return this;
    }

    public ItemBuilder removeEnchantment(@Nonnull final Enchantment enchantment) {
        this.meta.removeEnchant(enchantment);
        return this;
    }

    public ItemBuilder addItemFlag(@Nonnull final ItemFlag itemFlag) {
        this.meta.addItemFlags(new ItemFlag[] { itemFlag });
        return this;
    }

    public ItemBuilder removeItemFlag(@Nonnull final ItemFlag itemFlag) {
        this.meta.removeItemFlags(new ItemFlag[] { itemFlag });
        return this;
    }

    public ItemBuilder addAttributeModifier(@Nonnull final Attribute attribute, @Nonnull final AttributeModifier attributeModifier) {
        this.meta.addAttributeModifier(attribute, attributeModifier);
        return this;
    }

    public ItemBuilder removeAttributeModifier(@Nonnull final Attribute attribute) {
        this.meta.removeAttributeModifier(attribute);
        return this;
    }

    public ItemBuilder removeAttributeModifier(@Nonnull final Attribute attribute, @Nonnull final AttributeModifier attributeModifier) {
        this.meta.removeAttributeModifier(attribute, attributeModifier);
        return this;
    }

    public ItemBuilder setEnchantmentGlow(final boolean shouldGlow) {
        if (shouldGlow) {
            this.meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            this.addItemFlag(ItemFlag.HIDE_ENCHANTS);
        } else {
            this.meta.getEnchants().forEach((enchant, level) -> this.meta.removeEnchant(enchant));
        }
        return this;
    }

    public ItemBuilder addPersistentData(@Nonnull final Plugin plugin, @Nonnull final String key, @Nonnull final String value) {
        final NamespacedKey k = new NamespacedKey(plugin, key);
        this.meta.getPersistentDataContainer().set(k, PersistentDataType.STRING, value);
        return this;
    }

    public ItemBuilder removePersistentData(@Nonnull final Plugin plugin, @Nonnull final String key) {
        final NamespacedKey k = new NamespacedKey(plugin, key);
        try {
            this.meta.getPersistentDataContainer().remove(k);
        } catch (final NullPointerException e) {
            throw new IllegalArgumentException("Error while trying to remove NBT tag: The item does not contain such key '" + key + "'!");
        }
        return this;
    }
}
