/*
 * Copyright (c) 2022, Jamal <http://github.com/1Defence>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.example;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.util.Text;

public class PartyHealthStatusOverlay extends Overlay
{
    private final Client client;
    private final PartyHealthStatusConfig config;
    private final PartyHealthStatusPlugin plugin;

    @Inject
    PartyHealthStatusOverlay(Client client, PartyHealthStatusConfig config, PartyHealthStatusPlugin plugin)
    {
        this.client = client;
        this.config = config;
        this.plugin = plugin;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ALWAYS_ON_TOP);
    }

    int ClampMax(float val, float max){
        return val > max ? (int)max : (int)val;
    }

    float ClampMinf(float val, float min){
        return val < min ? min : val;
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {

        graphics.setFont(new Font(FontManager.getRunescapeFont().toString(), Font.BOLD, this.config.fontSize()));

        //track player locations for vertical-offsetting purposes, when players are stacked their names/hp(if rendered) should stack instead of overlapping
        List<WorldPoint> trackedLocations = new ArrayList<>();


        for(Player player : client.getPlayers()){

            if (player == null || player.getName() == null)
            {
                continue;
            }

            String name = player.getName();
            if(!plugin.getMembers().containsKey(name)){
                continue;
            }

            if(!plugin.getVisiblePlayers().isEmpty() && !plugin.getVisiblePlayers().contains(name.toLowerCase())){
                continue;
            }

            PartyHealthStatusMember memberData = plugin.getMembers().get(name);
            int currentHP = memberData.getCurrentHP();
            int maxHP = memberData.getMaxHP();

            boolean nameRendered = config.drawNames() || (!config.drawNames() && (currentHP < maxHP));
            Color col = config.getHealthyColor();

            if(nameRendered){
                int playersTracked = 0;
                WorldPoint currentLoc = player.getWorldLocation();
                for(int i=0; i<trackedLocations.size(); i++){
                    WorldPoint compareLoc = trackedLocations.get(i);
                    if(compareLoc.getX() == currentLoc.getX() && compareLoc.getY() == currentLoc.getY()){
                        playersTracked++;
                    }
                }
                trackedLocations.add(player.getWorldLocation());

                float hpThreshold = config.getHitpointsThreshold();
                float currentRatio = (currentHP - hpThreshold <= 0) ? 0 : ClampMinf(((float)currentHP - hpThreshold) / maxHP, 0);
                int r = ClampMax((1 - currentRatio) * 255, 255);
                int g = ClampMax(currentRatio * 255, 255);
                col = (config.getHitpointsThreshold() == 0 || currentHP >= maxHP) ? config.getHealthyColor() : new Color(r, g, 0, config.hullOpacity());
                renderPlayerOverlay(graphics, player, col, playersTracked);

            }

            if(config.renderPlayerHull()) {
                Shape objectClickbox = player.getConvexHull();
                renderPoly(graphics, col, objectClickbox);
            }


        }

        return null;
    }

    private void renderPoly(Graphics2D graphics, Color color, Shape shape)
    {
        if (shape != null)
        {
            graphics.setColor(color);
            graphics.setStroke(new BasicStroke(2));
            graphics.draw(shape);
            graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), config.hullOpacity()));
            graphics.fill(shape);
        }
    }

    private void renderPlayerOverlay(Graphics2D graphics, Player actor, Color color, int playersTracked)
    {
        int currentHP = plugin.getMembers().get(actor.getName()).getCurrentHP();
        int maxHP = plugin.getMembers().get(actor.getName()).getMaxHP();

        String playerName = config.drawNames() ? Text.removeTags(actor.getName()) : "";
        String endingPercentString = config.drawPercentByName() ? "%" : "";
        String startingParenthesesString = config.drawParentheses() ? "(" : "";
        String endingParenthesesString = config.drawParentheses() ? ")" : "";

        if(currentHP != -1){
            playerName += currentHP >= maxHP ? "" : " "+(startingParenthesesString+(currentHP*100)/maxHP)+endingPercentString+endingParenthesesString;
        }

        Point textLocation = actor.getCanvasTextLocation(graphics, playerName, config.offSetTextZ()/*(playersTracked*20)*/);

        float verticalOffSetMultiplier = 1f + (playersTracked*0.1f);

        if(textLocation != null)
        {
            textLocation = new Point(textLocation.getX() + config.offSetTextHorizontal(), (-config.offSetTextVertial())+(int) (textLocation.getY() * verticalOffSetMultiplier));
            OverlayUtil.renderTextLocation(graphics, textLocation, playerName, color);
        }

    }





}
