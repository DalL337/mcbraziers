package com.dai.mcbraziers.client;

import com.dai.mcbraziers.block.FoodLayout;

/** Added to vanilla's campfire render state by a mixin: where its food is drawn. */
public interface BrazierFoodLayout {

    FoodLayout mcbraziers$foodLayout();

    void mcbraziers$setFoodLayout(FoodLayout layout);
}
