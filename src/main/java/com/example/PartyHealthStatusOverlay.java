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
    private final PartyHealthStatusPlugin plugin;

    @Inject
    PartyHealthStatusOverlay(Client client, PartyHealthStatusPlugin plugin)
    {
        this.client = client;
        this.plugin = plugin;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.UNDER_WIDGETS);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        //skip redundant checking
        if(plugin.hideAllPlayers){
            return null;
        }

        graphics.setFont(new Font(FontManager.getRunescapeFont().toString(), plugin.boldFont ? Font.BOLD : Font.PLAIN, plugin.fontSize));

        //track player locations for vertical-offsetting purposes, when players are stacked their names/hp(if rendered) should stack instead of overlapping
        List<WorldPoint> trackedLocations = new ArrayList<>();


        for(Player player : client.getTopLevelWorldView().players()){

            if (player == null || player.getName() == null)
            {
                continue;
            }

            String name = plugin.SanitizeName(player.getName());
            if(!plugin.RenderPlayer(name)){
                continue;
            }


            int currentHP = plugin.getMembers().get(name).getCurrentHP();
            int maxHP = plugin.getMembers().get(name).getMaxHP();

            boolean healthy = plugin.IsHealthy(currentHP,maxHP);

            boolean nameRendered = plugin.RenderText(plugin.nameRender,healthy) || plugin.RenderText(plugin.hpRender,healthy);
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
                    col = plugin.GetHitPointsColor(currentHP,maxHP);
                }

                col = new Color(col.getRed(),col.getGreen(),col.getBlue(),plugin.hullOpacity);

                renderPlayerOverlay(graphics, player, col, playersTracked,currentHP,maxHP,healthy);

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

    private void renderPlayerOverlay(Graphics2D graphics, Player actor, Color color, int playersTracked, int currentHP, int maxHP, boolean healthy)
    {
        String playerName = plugin.RenderText(plugin.nameRender,healthy) ? Text.removeTags(actor.getName()) : "";
        String endingPercentString = plugin.drawPercentByName ? "%" : "";
        String startingParenthesesString = plugin.drawParentheses ? "(" : "";
        String endingParenthesesString = plugin.drawParentheses ? ")" : "";

        int healthValue = plugin.drawPercentByName ? ((currentHP*100)/maxHP) : currentHP;

        playerName += plugin.RenderText(plugin.hpRender,healthy) ? " "+(startingParenthesesString+healthValue+endingPercentString+endingParenthesesString) : "";


        Point textLocation = actor.getCanvasTextLocation(graphics, playerName, plugin.offSetTextZ/*(playersTracked*20)*/);

        float verticalOffSetMultiplier = 1f + (playersTracked * (((float)plugin.offSetStackVertical)/100f));

        if(textLocation != null)
        {
            textLocation = new Point(textLocation.getX() + plugin.offSetTextHorizontal, (-plugin.offSetTextVertical)+(int) (textLocation.getY() * verticalOffSetMultiplier));
            OverlayUtil.renderTextLocation(graphics, textLocation, playerName, color);
        }

    }





}
