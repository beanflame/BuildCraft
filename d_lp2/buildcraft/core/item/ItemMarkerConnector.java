package buildcraft.core.item;

import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import buildcraft.core.lib.utils.Utils;
import buildcraft.lib.item.ItemBuildCraft_BC8;
import buildcraft.lib.misc.PositionUtil;
import buildcraft.lib.misc.PositionUtil.Line;
import buildcraft.lib.tile.MarkerCache;
import buildcraft.lib.tile.TileMarkerBase;

public class ItemMarkerConnector extends ItemBuildCraft_BC8 {
    public ItemMarkerConnector(String id) {
        super(id);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(ItemStack stack, World world, EntityPlayer player, EnumHand hand) {
        EnumActionResult result = EnumActionResult.PASS;
        if (!world.isRemote) {
            for (MarkerCache<?> cache : TileMarkerBase.CACHES) {
                if (interactCache(cache, world, player)) {
                    result = EnumActionResult.SUCCESS;
                    player.swingArm(hand);
                    break;
                }
            }
        }
        return ActionResult.newResult(result, stack);
    }

    private static <T extends TileMarkerBase<T>> boolean interactCache(MarkerCache<T> cache, World world, EntityPlayer player) {
        MarkerLineInteraction<T> best = null;
        Map<BlockPos, T> map = cache.getCache(world);
        Vec3d playerPos = player.getPositionVector().addVector(0, player.getEyeHeight(), 0);
        Vec3d playerLookPos = playerPos.add(PositionUtil.scale(player.getLookVec(), 3));
        for (T marker : map.values()) {
            for (T possible : marker.getValidConnections()) {
                MarkerLineInteraction<T> interaction = new MarkerLineInteraction<>(marker, possible, playerPos, playerLookPos);
                if (interaction.didInteract(player)) {
                    best = interaction.getBetter(best);
                }
            }
        }
        if (best != null) {
            return best.marker1.tryConnectTo(best.marker2);
        }
        return false;
    }

    private static class MarkerLineInteraction<T extends TileMarkerBase<T>> {
        public final T marker1, marker2;
        public final double distToPoint;

        public MarkerLineInteraction(T marker1, T marker2, Vec3d playerPos, Vec3d playerEndPos) {
            this.marker1 = marker1;
            this.marker2 = marker2;
            Line line = new Line(new Vec3d(marker1.getPos()).add(Utils.VEC_HALF), new Vec3d(marker2.getPos()).add(Utils.VEC_HALF));
            Vec3d interactionPoint = PositionUtil.rayTrace(line, playerPos, playerEndPos);
            distToPoint = interactionPoint.distanceTo(playerPos);
        }

        public boolean didInteract(EntityPlayer player) {
            return distToPoint <= 3;
        }

        public MarkerLineInteraction<T> getBetter(MarkerLineInteraction<T> other) {
            if (other == null) return this;
            if (other.marker1 == marker2 && other.marker2 == marker1) {
                return other;
            }
            if (other.distToPoint < distToPoint) return other;
            return this;
        }
    }
}