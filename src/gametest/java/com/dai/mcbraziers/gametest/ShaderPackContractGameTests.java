package com.dai.mcbraziers.gametest;

import com.dai.mcbraziers.block.BrazierBlock;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * The names shader packs match to give braziers coloured light ({@code docs/plans/shaders/brief-shader-pack-contract.md}).
 * Expected values are copied from the {@code block.properties} entries merged into Solas (Septonious/Solas-Shader#206);
 * the Photon PR (sixthsurge/photon#692) uses the same entries. Each is resolved the way Iris resolves it: the tag by id
 * from the live registry, each filter's property by name, and its value with {@link Property#getValue(String)}.
 */
public class ShaderPackContractGameTests {

    // %mcbraziers:braziers:fire=<fire>:lit=true, one entry per fire type
    private static final String BRAZIER_TAG = "mcbraziers:braziers";
    private static final List<String> FIRES = List.of("normal", "soul", "copper", "amethyst");
    // mcbraziers:copper_campfire:lit=true and mcbraziers:amethyst_campfire:lit=true
    private static final List<String> CAMPFIRES = List.of("mcbraziers:copper_campfire", "mcbraziers:amethyst_campfire");

    @GameTest
    public void brazierTagEntriesMatchEveryBrazier(GameTestHelper helper) {
        TagKey<Block> tag = TagKey.create(Registries.BLOCK, Identifier.parse(BRAZIER_TAG));
        int tagged = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            String id = BuiltInRegistries.BLOCK.getKey(block).toString();
            boolean inTag = block.defaultBlockState().is(tag);
            // a brazier left out of the tag gets no shader light at all
            helper.assertTrue(inTag || !(block instanceof BrazierBlock), id + " is a brazier but not in " + BRAZIER_TAG);
            if (inTag) {
                tagged++;
                for (String fire : FIRES) {
                    assertFilterMatches(helper, block, id, "fire", fire);
                }
                assertFilterMatches(helper, block, id, "lit", "true");
            }
        }
        helper.assertTrue(tagged > 0, BRAZIER_TAG + " is missing or empty");
        helper.succeed();
    }

    @GameTest
    public void campfireEntriesMatch(GameTestHelper helper) {
        for (String id : CAMPFIRES) {
            Block block = BuiltInRegistries.BLOCK.getOptional(Identifier.parse(id)).orElse(null);
            helper.assertTrue(block != null, "no block " + id);
            assertFilterMatches(helper, block, id, "lit", "true");
        }
        helper.succeed();
    }

    /** Iris drops a filter whose property the block lacks, so the entry matches every state: wrong colour, no error. */
    private static void assertFilterMatches(GameTestHelper helper, Block block, String id, String property, String value) {
        Property<?> found = block.getStateDefinition().getProperty(property);
        helper.assertTrue(found != null, id + " has no '" + property + "' property");
        helper.assertTrue(found.getValue(value).isPresent(), id + " has no " + property + "=" + value);
    }
}
