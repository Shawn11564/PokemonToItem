
package me.sreeraj.pokemontoitem.screen;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import me.sreeraj.pokemontoitem.commands.PokeToItem;
import me.sreeraj.pokemontoitem.util.PokemonUtility;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class PokeToItemHandlerFactory implements NamedScreenHandlerFactory {

    public final PokeToItem.Session session;



    public PokeToItemHandlerFactory(PokeToItem.Session session) {
        this.session = session;
    }
    @Override
    public Text getDisplayName() {
        return Text.of("Pokemon To Item Screen");
    }

    int rows() {
        return 4;
    }

    int size() {
        return rows() * 9;
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        SimpleInventory inventory = new SimpleInventory(size());
        for (int i = 0; i < size(); i++) {
            inventory.setStack(i, new ItemStack(Items.GRAY_STAINED_GLASS_PANE).setCustomName(Text.of(" ")));
        }

        PlayerPartyStore storage = Cobblemon.INSTANCE.getStorage().getParty(session.sPlayer);
        for (int i = 0; i < 6; i++) {
            Pokemon pokemon = storage.get(i);
            if (pokemon != null) {
                ItemStack item = PokemonUtility.pokemonToItem(pokemon);
                NbtCompound slotNbt = item.getOrCreateSubNbt("slot");
                slotNbt.putInt("slot", i);
                item.setSubNbt("slot", slotNbt);
                inventory.setStack(12 + i + (i >= 3 ? 6 : 0), item);
            }
            else{inventory.setStack(12 + i + (i >= 3 ? 6 : 0), ItemStack.EMPTY);}

        }

        return new GenericContainerScreenHandler(ScreenHandlerType.GENERIC_9X4, syncId, inv, inventory, rows()) {
            @Override
            public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
                int emptySlot;
                ItemStack stack = getInventory().getStack(slotIndex);
                if (stack != null && stack.hasNbt() && stack.getSubNbt("slot") != null) {
                    int slot = stack.getSubNbt("slot").getInt("slot");

                    Pokemon pokemon = Cobblemon.INSTANCE.getStorage().getParty(session.sPlayer).get(slot);
                    NbtCompound nbt = pokemon.saveToNBT(new NbtCompound());


                    ItemStack pokemonItem = PokemonUtility.pokemonToItem(pokemon);
                    NbtCompound pokemonNbt = pokemonItem.getOrCreateSubNbt("PokemonData");
                    pokemonNbt.put("Data", nbt);
                    pokemonItem.setSubNbt("PokemonData", pokemonNbt);
                    emptySlot = player.getInventory().getEmptySlot();
                    if (emptySlot!=-1){
                        player.giveItemStack(pokemonItem);}
                    else {
                        player.dropStack(pokemonItem);
                    }
                    session.removePokemon(pokemon);
                    inventory.setStack(12 + slot + (slot >= 3 ? 6 : 0), ItemStack.EMPTY);

                    ScreenHandlerSlotUpdateS2CPacket packet = new ScreenHandlerSlotUpdateS2CPacket(syncId, nextRevision(), 12 + slot + (slot >= 3 ? 6 : 0), ItemStack.EMPTY);
                    session.sPlayer.networkHandler.sendPacket(packet);
                }
            }
            @Override
            public ItemStack quickMove(PlayerEntity player, int index) {
                return null;
            }

            @Override
            public boolean canInsertIntoSlot(Slot slot) {
                return true;
            }
        };
    }
}

