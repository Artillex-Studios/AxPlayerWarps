package com.artillexstudios.axplayerwarps.placeholders.resolvers;

import com.artillexstudios.axapi.placeholders.PlaceholderArgumentResolver;
import com.artillexstudios.axplayerwarps.warps.Warp;
import com.artillexstudios.axplayerwarps.warps.WarpManager;

import javax.annotation.Nullable;

public class WarpResolver implements PlaceholderArgumentResolver<Warp> {

    @Nullable
    @Override
    public Warp resolve(String string) {
        return WarpManager.getWarp(string);
    }
}