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

import net.runelite.client.config.*;

import java.awt.*;

@ConfigGroup("partyhealthstatus")
public interface PartyHealthStatusConfig extends Config
{

	enum ColorType { LERP_2D,LERP_3D,COLOR_THRESHOLDS,STATIC}
	enum TextRenderType { ALWAYS,NEVER,WHEN_MISSING_HP}
	@ConfigSection(name="Color Thresholds", description="Values used when Color Type is Color Thresholds", position=2, closedByDefault=true)
	String colorThresholds = "customThresholds";
	@ConfigSection(name="Color Lerp 2d", description="Values used when Color Type is Lerp 2d", position=2, closedByDefault=true)
	String colorLerp2d = "colorLerp2d";
	@ConfigSection(name="Visual Overlay", description="visual overlay settings", position=1, closedByDefault=false)
	String visualOverlay = "visualOverlay";
	@ConfigSection(name="Text Overlay", description="text overlay settings", position=2, closedByDefault=true)
	String textOverlay = "textOverlay";

	/*Visual Overlay*/
	@ConfigItem(
			position = 0,
			keyName = "visiblePlayers",
			name = "Visible Players",
			description = "Only names listed will have visuals shown, if list is empty all connected party members will show up",
			section = visualOverlay
	)
	default String getVisiblePlayers()
	{
		return "";
	}

	@Alpha
	@ConfigItem(
			position = 1,
			keyName = "healthyColor",
			name = "Healthy Color",
			description = "The default color of a healthy full-HP player",
			section = visualOverlay
	)
	default Color getHealthyColor()
	{
		return new Color(255,255,255,50);
	}
	@ConfigItem(
			position = 2,
			keyName = "healthyOffset",
			name = "Healthy Offset",
			description = "The offset from maximum hp to render an account as healthy/full-HP, ex 10 @99hp would be 89",
			section = visualOverlay)
	default int healthyOffset()
	{
		return 0;
	}

	@ConfigItem(
			position = 3,
			keyName = "hullOpacity",
			name = "Hull Opacity",
			description = "hull opcacity, 30 recommended",
			section = visualOverlay)
	default int hullOpacity() { return 30; }

	@ConfigItem(
			position = 4,
			keyName = "renderPlayerHull",
			name = "Render Player Hull",
			description = "Render the hull of visible party members",
			section = visualOverlay)
	default boolean renderPlayerHull()
	{
		return false;
	}

	@ConfigItem(
			position = 5,
			keyName = "recolorHealOther",
			name = "Recolor Heal Other",
			description = "Recolor heal other menus based on current hitpoints, grey will indicate the member is healthy/full hitpoints",
			section = visualOverlay)
	default boolean recolorHealOther()
	{
		return false;
	}

	@ConfigItem(
			position = 6,
			keyName = "colorType",
			name = "Color Type",
			description = "Method of color calculation",
			section = visualOverlay
	)
	default ColorType getColorType()
	{
		return ColorType.LERP_2D;
	}

	/*Color Thresholds*/
	@ConfigItem(
			position = 0,
			keyName = "highColor",
			name = "High Color",
			description = "The Color when party members hitpoints are below MAX hitpoints",
			section = colorThresholds
	)
	default Color getHighColor()
	{
		return new Color(0,255,0);
	}

	@Range(min = 1, max = 99)
	@ConfigItem(
			position = 1,
			keyName = "mediumHP",
			name = "Medium HP",
			description = "Hitpoints percentage to render the MEDIUM Color (at or below this number)",
			section = colorThresholds
	)
	default int getMediumHP()
	{
		return 70;
	}

	@ConfigItem(
			position = 2,
			keyName = "mediumColor",
			name = "Medium Color",
			description = "The Color when party members hitpoints are at or below the MEDIUM threshold",
			section = colorThresholds
	)
	default Color getMediumColor()
	{
		return new Color(255, 200, 0);
	}
	@Range(min = 1, max = 99)
	@ConfigItem(
			position = 3,
			keyName = "lowHP",
			name = "Low HP",
			description = "Hitpoints percentage to render the LOW Color (at or below this number)",
			section = colorThresholds
	)
	default int getLowHP()
	{
		return 40;
	}

	@ConfigItem(
			position = 4,
			keyName = "lowColor",
			name = "Low Color",
			description = "The Color when party members hitpoints are at or below the LOW threshold",
			section = colorThresholds
	)
	default Color getLowColor()
	{
		return new Color(255, 0, 0);
	}

	/*Color Lerp 2d*/
	@Range(max = 40)
	@ConfigItem(
			position = 0,
			keyName = "hitPointsMinimum",
			name = "Hitpoints Minimum",
			description = "The amount of hitpoints the player should be highlighted fully red at(1-99), 20 recommended",
			section = colorLerp2d
	)
	default int getHitpointsMinimum()
	{
		return 20;
	}

	/*Text Overlay*/
	@ConfigItem(
			position = -2,
			keyName = "nameRender",
			name = "Name Render",
			description = "Configures how player names should render",
			section = textOverlay
	)
	default TextRenderType nameRender()
	{
		return TextRenderType.WHEN_MISSING_HP;
	}
	@ConfigItem(
			position = 0,
			keyName = "hpRender",
			name = "HP Render",
			description = "Configures how player hitpoints should render",
			section = textOverlay
	)
	default TextRenderType hpRender()
	{
		return TextRenderType.WHEN_MISSING_HP;
	}

	@ConfigItem(
			position = 1,
			keyName = "drawPercentByName",
			name = "Draw Percent By Name",
			description = "Draw a % beside the numeral value of remaining hp",
			section = textOverlay)
	default boolean drawPercentByName() { return false; }

	@ConfigItem(
			position = 2,
			keyName = "drawParentheses",
			name = "Draw Parentheses By Name",
			description = "Draw parentheses surrounding hp number",
			section = textOverlay)
	default boolean drawParentheses() { return false; }

	@ConfigItem(
			position = 3,
			keyName = "offSetTextHorizontal",
			name = "OffSet Text Horizontal",
			description = "OffSet the text horizontally",
			section = textOverlay)
	default int offSetTextHorizontal() { return 0; }

	@ConfigItem(
			position = 4,
			keyName = "offSetTextVertical",
			name = "OffSet Text Vertical",
			description = "OffSet the text vertically",
			section = textOverlay)
	default int offSetTextVertial() { return 0; }

	@ConfigItem(
			position = 5,
			keyName = "offSetTextZ",
			name = "OffSet Text Z",
			description = "OffSet the text Z",
			section = textOverlay)
	default int offSetTextZ() { return 65; }

	@Range(min = 1,max=20)
	@ConfigItem(
			position = 6,
			keyName = "offSetStackVertical",
			name = "OffSet Stack Vertical",
			description = "OffSet the text vertically when multiple accounts are stacked",
			section = textOverlay)
	default int offSetStackVertical() { return 10; }

	@Range(max=16, min=8)
	@ConfigItem(
			position=7,
			keyName="fontSize",
			name="Font Size",
			description="font size",
			section = textOverlay)
	default int fontSize() {
		return 12;
	}

	@ConfigItem(
			keyName = "boldFont",
			name = "Bold Font",
			description = "Configures whether font is bold or not",
			position = 8,
			section = textOverlay
	)
	default boolean boldFont()
	{
		return true;
	}

}
