package net.minecraft.scalar.rosettaenchanttable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public class Packet_mod_RosettaEnchantTable implements CustomPacketPayload {
	public static final Type<Packet_mod_RosettaEnchantTable> TYPE = new Type<>(
			ResourceLocation.fromNamespaceAndPath(mod_RosettaEnchantTable.MODID, "packet"));
	public static final StreamCodec<FriendlyByteBuf, Packet_mod_RosettaEnchantTable> STREAM_CODEC = 
			CustomPacketPayload.codec(Packet_mod_RosettaEnchantTable::writePacketData, Packet_mod_RosettaEnchantTable::readPacketData);
	public int randomSeed = 0;
	public int sendPacketCnt = 0;
	public boolean isResetLevel = false;
	public BlockPos _pos = BlockPos.ZERO;
	public Packet_mod_RosettaEnchantTable() {
	}

	public static Packet_mod_RosettaEnchantTable readPacketData(FriendlyByteBuf var1) {
		final var ret = new Packet_mod_RosettaEnchantTable();
		ret.randomSeed = var1.readInt();
		ret.sendPacketCnt = var1.readInt();
		ret.isResetLevel = var1.readBoolean();
		final var x = var1.readInt();
		final var y = var1.readInt();
		final var z = var1.readInt();
		ret._pos = new BlockPos(x, y, z);
		//System.out.println(this.toString());
		return ret;
	}

	public void writePacketData(FriendlyByteBuf var1) {
		var1.writeInt(this.randomSeed);
		var1.writeInt(this.sendPacketCnt);
		var1.writeBoolean(this.isResetLevel);
		var1.writeInt(this._pos.getX());
		var1.writeInt(this._pos.getY());
		var1.writeInt(this._pos.getZ());
	}

	@Override
	public String toString() {
		return String.format("Packet_mod_RosettaEnchantTable (seed=%d,cnt=%d)"
				, this.randomSeed, this.sendPacketCnt);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
