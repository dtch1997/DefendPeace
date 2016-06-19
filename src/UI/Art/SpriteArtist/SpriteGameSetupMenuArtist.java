package UI.Art.SpriteArtist;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import Terrain.Environment.Terrains;
import Terrain.GameMap;
import Terrain.MapInfo;
import Terrain.MapLibrary;
import UI.GameSetupController;

public class SpriteGameSetupMenuArtist
{
  private static Dimension dimensions = null;
  private static int drawScale = 3;

  private static final Color MENUFRAMECOLOR = new Color(169, 118, 65);
  private static final Color MENUBGCOLOR = new Color(234, 204, 154);
  private static final Color MENUHIGHLIGHTCOLOR = new Color(246, 234, 210);

  private static final int characterWidth = SpriteLibrary.getLettersUppercase().getFrame(0).getWidth()*drawScale;
  private static final int characterHeight = SpriteLibrary.getLettersUppercase().getFrame(0).getHeight()*drawScale;
  private static final int maxNameDisplayLength = 14;
  // The map-name section shall be wide enough for a 14-character name, plus two-pixel buffer on each side.
  // (characterWidth+1)*maxNameDisplayLength + 3 (3 not 4, because the kerning buffer for the final letter is 1 already).
  private static int nameSectionWidth =
      ((characterWidth+drawScale) * maxNameDisplayLength) + 3*drawScale;

  public static void setDimensions(Dimension d)
  {
    dimensions = d;
  }

  public static void draw(Graphics g, GameSetupController gameSetup)
  {
    if(null == dimensions )
    {
      // If we don't know how big we can draw, don't.
      System.out.println("Warning: SpriteGameSetupMenuArtist has no dimensions!");
      return;
    }

    int highlightedOption = gameSetup.getSelectedOption();

    /////////////// Map selection pane ///////////////////////
    // Paint the whole area over in our background color.
    g.setColor(MENUBGCOLOR);
    g.fillRect(0,0,dimensions.width,dimensions.height);

    // Draw the frame for the list of maps, with the highlight for the current option.
    int frameBorderHeight = 3*drawScale;
    g.setColor(MENUFRAMECOLOR);
    g.fillRect(nameSectionWidth, 0, drawScale, dimensions.height); // sidebar
    g.fillRect(0,0,nameSectionWidth,frameBorderHeight); // top bar
    g.fillRect(0, dimensions.height-frameBorderHeight, nameSectionWidth, 3*drawScale); // bottom bar
    //TODO: Draw little arrows on the top and bottom frame to show it can scroll.

    // Draw the highlight for the selected option.
    g.setColor(MENUHIGHLIGHTCOLOR);
    int menuTextYStart = frameBorderHeight + drawScale; // Upper frame border plus 1 pixel buffer.
    int menuOptionHeight = SpriteLibrary.getLettersUppercase().getFrame(0).getHeight()*drawScale;
    int selectedOptionYOffset = menuTextYStart + highlightedOption * (menuOptionHeight+1);
    g.fillRect(0, selectedOptionYOffset, nameSectionWidth, menuOptionHeight);

    // Get the list of selectable maps (possibly specifying a filter (#players, etc).
    ArrayList<MapInfo> mapInfos = MapLibrary.getMapList(); // = MapLibrary.getMapNames();

    // Display the names from the list, highlighting the one that is currently chosen.
    for(int i = 0; i < mapInfos.size(); ++i)
    {
      int drawX = 2; // Offset from the edge of the window slightly.
      int drawY = menuTextYStart + i*(menuOptionHeight+drawScale); // +drawScale for a buffer between options.

      // Draw visible map names in the list.
      String str = mapInfos.get(i).mapName;
      if(str.length() > maxNameDisplayLength)
      {
        str = str.substring(0, maxNameDisplayLength);
      }

      SpriteLibrary.drawText(g, str, drawX, drawY, drawScale);
    }

    /////////////// MiniMap ///////////////////////
    // Figure out how large the map can be based on the border divisions.
    int maxMiniMapWidth = dimensions.width - nameSectionWidth;
    int maxMiniMapHeight = 123*drawScale;

    // Find the center of the minimap display.
    int miniMapCenterX = nameSectionWidth + (maxMiniMapWidth / 2);
    int miniMapCenterY = maxMiniMapHeight / 2;

    // Draw a line to separate the minimap image and the player/property info.
    g.setColor(MENUFRAMECOLOR);
    g.fillRect(nameSectionWidth, maxMiniMapHeight, dimensions.width-nameSectionWidth, drawScale);

    // Draw the mini-map representation of the highlighted map.
    MapInfo mapInfo = mapInfos.get(highlightedOption);
    BufferedImage miniMap = SpriteMiniMapArtist.getMapImage( mapInfo );

    // Figure out how large to draw the minimap. We want to make it as large as possible, but still
    //   fit inside the available space (with a minimum scale factor of 1).
    int mmWScale = maxMiniMapWidth / miniMap.getWidth();
    int mmHScale = maxMiniMapHeight / miniMap.getHeight();
    int mmScale = (mmWScale > mmHScale)? mmHScale : mmWScale;

    // Draw the mini map.
    SpriteLibrary.drawImageCenteredOnPoint(g, miniMap, miniMapCenterX, miniMapCenterY, mmScale);

    /////////////// Map Information ///////////////////////]
    int buffer = 3*drawScale;
    int numPlayers = mapInfo.COProperties.length; // Get the number of players the map supports
    StringBuilder sb = new StringBuilder().append(numPlayers).append(" Players"); // Build a string to say that.
    SpriteLibrary.drawText(g, sb.toString(), nameSectionWidth+buffer, maxMiniMapHeight+buffer, drawScale);

    sb.setLength(0); // Clear so we can build the map dimensions string.
    sb.append(mapInfo.getWidth()).append("x").append(mapInfo.getHeight());
    int dimsDrawX = dimensions.width - (characterWidth*7) - drawScale;
    SpriteLibrary.drawText(g, sb.toString(), dimsDrawX, maxMiniMapHeight+buffer, drawScale);

    // Draw the number of each type of property on this map.
    int sqSize = SpriteLibrary.baseSpriteSize*drawScale; // Figure out how large "1 tile" is in pixels.
    int propsDrawX = nameSectionWidth + (2*buffer) - sqSize; // Map name pane plus generous buffer, minus one square (building sprites are 2x2).
    int propsDrawY = maxMiniMapHeight+buffer+characterHeight+buffer - sqSize; // Map  pane plus "# players" string plus buffer, minus one sq.

    // Define an array with all the property types we care to enumerate.
    Terrains[] propertyTypes = {Terrains.CITY, Terrains.FACTORY};
    for(int i = 0; i < propertyTypes.length; ++i)
    {
      Terrains terrain = propertyTypes[i];
      int num = countTiles(mapInfo, terrain);
      // Get the first image of the first variation of the specified terrain type.
      BufferedImage image = SpriteLibrary.getTerrainSpriteSet(terrain).getTerrainSprite().getFrame(0);
      // Draw it, with the number of times it occurs on the map.
      g.drawImage(image, propsDrawX, propsDrawY, sqSize*2, sqSize*2, null);

      drawPropertyNumber(g, num, propsDrawX, propsDrawY);

      // Increment x draw location a fair bit for the next one.
      propsDrawX += sqSize + 3*buffer;
    }
  }

  /**
   * Count and return the number of tiles of the provided type in the given MapInfo.
   */
  private static int countTiles(MapInfo mapInfo, Terrains tileType)
  {
    int count = 0;
    for(int y = 0; y < mapInfo.getHeight(); ++y)
    {
      for(int x = 0; x < mapInfo.getWidth(); ++x)
      {
        if(mapInfo.terrain[x][y] == tileType)
        {
          ++count;
        }
      }
    }
    return count;
  }

  /**
   * Utility function to draw a number on top of a property image.
   */
  private static void drawPropertyNumber(Graphics g, int num, int x, int y)
  {
    if(num > 99) // Cap it at 99 for now, and probably for ever.
    {
      System.out.println("INFO: Maps are getting large, yo");
      num = 99;
    }

    boolean dd = num > 9; // Is this a double-digit number?

    // Get the number images, and grab the dimensions
    Sprite nums = SpriteLibrary.getMapUnitHPSprites();
    int numWidth = nums.getFrame(0).getWidth() * drawScale;
    int numHeight = nums.getFrame(0).getHeight() * drawScale;

    // Property sprites are 2 tiles by two tiles, and we want to be right- and
    //   bottom-justified, so adjust sideways
    int propSize = SpriteLibrary.baseSpriteSize*drawScale*2;
    x += propSize - numWidth; // right-justify
    y += propSize - numHeight; // Bottom-justify - no double-digit adjustment.

    do // We divide by 10 and truncate each time; expect two loops max.
    {
      int frame = num % 10;
      g.drawImage(nums.getFrame(frame), x, y, numWidth, numHeight, null);
      num /= 10; // Shift to the next higher digit in the number.
      x -= numWidth; // Move the x-draw location to the left.
    }while(num > 0);
  }
}
