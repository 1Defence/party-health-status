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
import net.runelite.client.plugins.party.data.PartyData;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.Text;

public class PartyHealthStatusOverlay extends Overlay
{
    private final Client client;
    private final PartyHealthStatusPlugin plugin;

    @Inject
    PartyHealthStatusOverlay(Client client, PartyHealthStatusPlugin plugin)
    {
        this.client = client;
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
        graphics.setFont(new Font(FontManager.getRunescapeFont().toString(), plugin.boldFont ? Font.BOLD : Font.PLAIN, plugin.fontSize));

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

            long memberID = plugin.getMembers().get(name);
            PartyData partyData = plugin.getPartyPluginService().getPartyData(memberID);

            if (partyData == null){
                continue;
            }

            int currentHP = partyData.getHitpoints();
            int maxHP = partyData.getMaxHitpoints();

            boolean healthy = currentHP >= (maxHP-plugin.healthyOffSet);

            boolean nameRendered = plugin.drawNames || !healthy;
            Color col = plugin.healthyColor;

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

                if(!healthy){
                    switch (plugin.colorType){

                        case LERP_2D:
                        {
                            float hpThreshold = plugin.hitPointsMinimum;
                            float currentRatio = (currentHP - hpThreshold <= 0) ? 0 : ClampMinf(((float) currentHP - hpThreshold) / maxHP, 0);
                            int r = ClampMax((1 - currentRatio) * 255, 255);
                            int g = ClampMax(currentRatio * 255, 255);
                            col = new Color(r, g, 0, plugin.hullOpacity);
                        }
                            break;
                        case LERP_3D:
                        {
                            float halfHP = (float)maxHP/2f;
                            if(currentHP >= halfHP){
                                col = ColorUtil.colorLerp(Color.orange, Color.green, (((float)currentHP-halfHP)/halfHP));
                            }else{
                                col = ColorUtil.colorLerp(Color.red, Color.orange, (float)currentHP/halfHP);
                            }
                        }
                            break;
                        case COLOR_THRESHOLDS:
                        {
                            float hpPerc = ((float)currentHP/(float)maxHP)*maxHP;
                            col = hpPerc <= plugin.lowHP ? plugin.lowColor
                                    : hpPerc <= plugin.mediumHP ? plugin.mediumColor
                                    : hpPerc < maxHP ? plugin.highColor : plugin.healthyColor;
                        }
                            break;
                    }
                }

                col = new Color(col.getRed(),col.getGreen(),col.getBlue(),plugin.hullOpacity);

                renderPlayerOverlay(graphics, player, col, playersTracked,currentHP,maxHP);

            }

            if(plugin.renderPlayerHull) {
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
            graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), plugin.hullOpacity));
            graphics.fill(shape);
        }
    }

    private void renderPlayerOverlay(Graphics2D graphics, Player actor, Color color, int playersTracked, int currentHP, int maxHP)
    {
        String playerName = plugin.drawNames ? Text.removeTags(actor.getName()) : "";
        String endingPercentString = plugin.drawPercentByName ? "%" : "";
        String startingParenthesesString = plugin.drawParentheses ? "(" : "";
        String endingParenthesesString = plugin.drawParentheses ? ")" : "";

        int healthValue = plugin.drawPercentByName ? ((currentHP*100)/maxHP) : currentHP;

        if(currentHP != -1){
            playerName += currentHP >= (maxHP-plugin.healthyOffSet) ? "" : " "+(startingParenthesesString+healthValue+endingPercentString+endingParenthesesString);
        }

        Point textLocation = actor.getCanvasTextLocation(graphics, playerName, plugin.offSetTextZ/*(playersTracked*20)*/);

        float verticalOffSetMultiplier = 1f + (playersTracked * (((float)plugin.offSetStackVertical)/100f));

        if(textLocation != null)
        {
            textLocation = new Point(textLocation.getX() + plugin.offSetTextHorizontal, (-plugin.offSetTextVertical)+(int) (textLocation.getY() * verticalOffSetMultiplier));
            OverlayUtil.renderTextLocation(graphics, textLocation, playerName, color);
        }

    }





}
