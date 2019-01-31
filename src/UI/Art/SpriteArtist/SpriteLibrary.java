package UI.Art.SpriteArtist;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.ImageIcon;

import CommandingOfficers.Commander;
import CommandingOfficers.CommanderLibrary;
import Engine.XYCoord;
import Terrain.Location;
import Terrain.TerrainType;
import UI.COSetupController;
import UI.Art.SpriteArtist.SpriteUIUtils.ImageFrame;
import Units.Unit;
import Units.UnitModel;

/**
 * Responsible for loading all game images from disk. All methods are static, and resources are loaded the first time they are needed.
 */
public class SpriteLibrary
{
  // This is the physical size of a single map square in pixels.
  public static final int baseSpriteSize = 16;

  // Define extra colors as needed.
  private static final Color PURPLE = new Color(231, 123, 255);

  // Map Building colors.
  public static final Color[] defaultMapColors = { new Color(40, 40, 40), new Color(70, 70, 70), new Color(110, 110, 110),
      new Color(160, 160, 160), new Color(200, 200, 200), new Color(231, 231, 231) };
  private static Color[] pinkMapBuildingColors = { new Color(142, 26, 26), new Color(255, 219, 74), new Color(190, 90, 90),
      new Color(240, 140, 140), new Color(250, 190, 190), new Color(255, 245, 245) };
  private static Color[] cyanMapBuildingColors = { new Color(0, 105, 105), new Color(255, 219, 74), new Color(77, 157, 157),
      new Color(130, 200, 200), new Color(200, 230, 230), new Color(245, 255, 255) };
  private static Color[] orangeMapBuildingColors = { new Color(130, 56, 0), new Color(255, 237, 29), new Color(139, 77, 20),
      new Color(231, 139, 41), new Color(243, 186, 121), new Color(255, 234, 204) };
  private static Color[] purpleMapBuildingColors = { new Color(90, 14, 99), new Color(255, 207, 95), new Color(133, 65, 130),
      new Color(174, 115, 189), new Color(222, 171, 240), new Color(255, 231, 255) };

  // Map Unit colors.
  private static Color[] pinkMapUnitColors = { new Color(142, 26, 26), new Color(199, 62, 62), new Color(248, 100, 100),
      new Color(255, 136, 136), new Color(255, 175, 175), new Color(255, 201, 201) };
  private static Color[] cyanMapUnitColors = { new Color(0, 105, 105), new Color(0, 170, 170), new Color(0, 215, 215),
      new Color(0, 245, 245), new Color(121, 255, 255), new Color(195, 255, 255), };
  private static Color[] orangeMapUnitColors = { new Color(130, 56, 0), new Color(204, 103, 7), new Color(245, 130, 14),
      new Color(255, 160, 30), new Color(255, 186, 60), new Color(255, 225, 142), };
  private static Color[] purpleMapUnitColors = { new Color(90, 14, 99), new Color(132, 41, 148), new Color(181, 62, 198),
      new Color(201, 98, 223), new Color(231, 123, 255), new Color(243, 180, 255), };

  private static HashMap<Color, ColorPalette> buildingColorPalettes = new HashMap<Color, ColorPalette>(){
    private static final long serialVersionUID = 1L;
    {
      // Create a mapping of game colors to the fine-tuned colors that will be used for map sprites.
      put(Color.PINK, new ColorPalette(pinkMapBuildingColors));
      put(Color.CYAN, new ColorPalette(cyanMapBuildingColors));
      put(Color.ORANGE, new ColorPalette(orangeMapBuildingColors));
      put(PURPLE, new ColorPalette(purpleMapBuildingColors));
    }
  };
  private static HashMap<Color, ColorPalette> mapUnitColorPalettes = new HashMap<Color, ColorPalette>(){
    private static final long serialVersionUID = 1L;
    {
      // Create a mapping of game colors to the fine-tuned colors that will be used for map sprites.
      put(Color.PINK, new ColorPalette(pinkMapUnitColors));
      put(Color.CYAN, new ColorPalette(cyanMapUnitColors));
      put(Color.ORANGE, new ColorPalette(orangeMapUnitColors));
      put(PURPLE, new ColorPalette(purpleMapUnitColors));
    }
  };

  public static final Color[] coColorList = { Color.PINK, Color.CYAN, Color.ORANGE, PURPLE };

  // TODO: Account for weather?
  private static HashMap<SpriteSetKey, TerrainSpriteSet> spriteSetMap = new HashMap<SpriteSetKey, TerrainSpriteSet>();
  // TODO: Consider templatizing the key types, and then combining these two maps.
  private static HashMap<UnitSpriteSetKey, UnitSpriteSet> mapUnitSpriteSetMap = new HashMap<UnitSpriteSetKey, UnitSpriteSet>();

  public static Pattern factionNameToKey = Pattern.compile("(.+)\\s(.+)");

  // Sprites to hold the images for drawing tentative moves on the map.
  private static Sprite moveCursorLineSprite = null;
  private static Sprite moveCursorArrowSprite = null;

  // HP numbers to overlay on map units when damaged.
  private static Sprite mapUnitHPSprites = null;

  // Cargo icon for when transports are holding other units.
  private static BufferedImage mapUnitCargoIcon = null;

  // Capture icon for when units are capturing properties.
  private static BufferedImage mapUnitCaptureIcon = null;

  // Letters for writing in menus.
  private static Sprite letterSpritesUppercase = null;
  private static Sprite letterSpritesLowercase = null;
  private static Sprite numberSprites = null;

  // Letters for writing in menus.
  private static Sprite letterSpritesSmallCaps = null;
  private static Sprite numberSpritesSmallCaps = null;

  // Commander overlay backdrops (shows commander name and funds) for each Commander in the game.
  private static HashMap<Commander, Sprite> coOverlays = new HashMap<Commander, Sprite>();
  private static HashMap<Commander, Sprite> coPowerBarPieces = new HashMap<Commander, Sprite>();

  private static BufferedImage actionCursor = null;

  // Text images for main menu options.
  private static Sprite menuOptionsSprite = null;

  // Victory/Defeat text for game end.
  private static BufferedImage gameOverDefeatText = null;
  private static BufferedImage gameOverVictoryText = null;

  /**
   * Retrieve (loading if needed) the sprites associated with the given terrain type. For ownable terrain types
   * (e.g. cities), the unowned variant of the sprite will be returned.
   */
  public static TerrainSpriteSet getTerrainSpriteSet(TerrainType terrain)
  {
    SpriteSetKey spriteKey = SpriteSetKey.instance(terrain, null);
    if( !spriteSetMap.containsKey(spriteKey) )
    {
      // Don't have it? Create it.
      createTerrainSpriteSet(spriteKey);
    }
    // Now we must have an entry for that terrain type.
    return spriteSetMap.get(spriteKey);
  }

  /**
   * Retrieve (loading if needed) the sprites associated with the terrain type at the specified location.
   * This function returns the owner-appropriate version of the tile for ownable terrain types.
   */
  public static TerrainSpriteSet getTerrainSpriteSet(Location loc)
  {
    SpriteSetKey spriteKey = SpriteSetKey.instance(loc.getEnvironment().terrainType, loc.getOwner());
    if( !spriteSetMap.containsKey(spriteKey) )
    {
      // Don't have it? Create it.
      createTerrainSpriteSet(spriteKey);
    }
    // Now we must have an entry for that terrain type.
    return spriteSetMap.get(spriteKey);
  }

  /**
   * Loads the images and builds the TerrainSpriteSet for the terrain type passed in.
   * If we are unable to load the correct images for any reason, make a blank TerrainSpriteSet.
   * @param spriteKey Specifies the terrain type and owner, so we can build the correct image.
   */
  private static void createTerrainSpriteSet(SpriteSetKey spriteKey)
  {
    TerrainType terrainType = spriteKey.terrainKey;
    System.out.println("INFO: Loading terrain sprites for " + terrainType);

    TerrainSpriteSet ss = null;
    int w = baseSpriteSize;
    int h = baseSpriteSize;

    // Load the appropriate images and define the necessary relationships for each tile type.
    if( terrainType == TerrainType.BRIDGE )
    {
      ss = new TerrainSpriteSet(spriteKey.terrainKey, loadSpriteSheetFile("res/tileset/bridge_clear.png"), w, h);
      ss.addTerrainAffinity(TerrainType.GRASS); // No need to also add ROAD, since GRASS is the base of ROAD.
      ss.denyTerrainAffinity(TerrainType.RIVER); // RIVER has a base of GRASS, but we don't want bridge to tile with it.
    }
    else if( terrainType == TerrainType.CITY )
    {
      ss = new TerrainSpriteSet(spriteKey.terrainKey, loadSpriteSheetFile("res/tileset/city_clear.png"), w * 2, h * 2);
    }
    else if( terrainType == TerrainType.DUNES )
    {}
    else if( terrainType == TerrainType.FACTORY )
    {
      ss = new TerrainSpriteSet(spriteKey.terrainKey, loadSpriteSheetFile("res/tileset/factory_clear.png"), w * 2, h * 2);
    }
    else if( terrainType == TerrainType.AIRPORT )
    {
      ss = new TerrainSpriteSet(spriteKey.terrainKey, loadSpriteSheetFile("res/tileset/airport_clear.png"), w * 2, h * 2);
    }
    else if( terrainType == TerrainType.SEAPORT )
    {
      ss = new TerrainSpriteSet(spriteKey.terrainKey, loadSpriteSheetFile("res/tileset/seaport_clear.png"), w * 2, h * 2);
    }
    else if( terrainType == TerrainType.FOREST )
    {
      ss = new TerrainSpriteSet(spriteKey.terrainKey, loadSpriteSheetFile("res/tileset/forest_clear.png"), w * 2, h * 2);
    }
    else if( terrainType == TerrainType.GRASS )
    {
      ss = new TerrainSpriteSet(spriteKey.terrainKey, loadSpriteSheetFile("res/tileset/grass_clear.png"), w, h);
      ss.addTileTransition(TerrainType.BRIDGE, loadSpriteSheetFile("res/tileset/grass_bridge_clear.png"), w, h);
    }
    else if( terrainType == TerrainType.HEADQUARTERS )
    {
      ss = new TerrainSpriteSet(spriteKey.terrainKey, loadSpriteSheetFile("res/tileset/hq_clear.png"), w * 2, h * 2);
    }
    else if( terrainType == TerrainType.LAB )
    {
      // TODO: get actual sprites for this
      ss = new TerrainSpriteSet(spriteKey.terrainKey, loadSpriteSheetFile("res/tileset/hq_clear.png"), w * 2, h * 2);
    }
    else if( terrainType == TerrainType.MOUNTAIN )
    {
      ss = new TerrainSpriteSet(spriteKey.terrainKey, loadSpriteSheetFile("res/tileset/mountain_clear.png"), w * 2, h * 2);
    }
    else if( terrainType == TerrainType.SEA )
    {
      ss = new TerrainSpriteSet(spriteKey.terrainKey, loadSpriteSheetFile("res/tileset/sea_clear.png"), w, h);
      ss.addTileTransition(TerrainType.GRASS, loadSpriteSheetFile("res/tileset/sea_grass_clear.png"), w, h);
      ss.addTileTransition(TerrainType.BRIDGE, loadSpriteSheetFile("res/tileset/sea_bridge_clear.png"), w, h);
      ss.addTileTransition(TerrainType.RIVER, loadSpriteSheetFile("res/tileset/sea_river_clear.png"), w, h);
    }
    else if( terrainType == TerrainType.REEF )
    {
      ss = new TerrainSpriteSet(spriteKey.terrainKey, loadSpriteSheetFile("res/tileset/reef_clear.png"), w, h);
      ss.addTileTransition(TerrainType.SEA, loadSpriteSheetFile("res/tileset/reef_clear.png"), w, h);
    }
    else if( terrainType == TerrainType.RIVER )
    {
      ss = new TerrainSpriteSet(spriteKey.terrainKey, loadSpriteSheetFile("res/tileset/river_clear.png"), w, h);
      ss.addTileTransition(TerrainType.BRIDGE, loadSpriteSheetFile("res/tileset/sea_bridge_clear.png"), w, h);
      ss.addTerrainAffinity(TerrainType.SHOAL);
      ss.addTerrainAffinity(TerrainType.SEA);
    }
    else if( terrainType == TerrainType.ROAD )
    {
      ss = new TerrainSpriteSet(spriteKey.terrainKey, loadSpriteSheetFile("res/tileset/road_clear.png"), w, h);
      ss.addTerrainAffinity(TerrainType.BRIDGE);
      ss.addTerrainAffinity(TerrainType.HEADQUARTERS);
      ss.addTerrainAffinity(TerrainType.FACTORY);
    }
    else if( terrainType == TerrainType.SHOAL )
    {
      ss = new TerrainSpriteSet(spriteKey.terrainKey, loadSpriteSheetFile("res/tileset/shoal_clear.png"), w, h);
      ss.addTileTransition(TerrainType.SEA, loadSpriteSheetFile("res/tileset/shoal_sea_clear.png"), w, h);
      ss.addTileTransition(TerrainType.GRASS, loadSpriteSheetFile("res/tileset/shoal_grass_clear.png"), w, h);
      ss.addTileTransition(TerrainType.BRIDGE, loadSpriteSheetFile("res/tileset/sea_bridge_clear.png"), w, h);
      ss.addTileTransition(TerrainType.RIVER, loadSpriteSheetFile("res/tileset/shoal_river_clear.png"), w, h);
    }
    else
    {
      System.out.println("ERROR! [SpriteLibrary.loadTerrainSpriteSet] Unknown terrain type " + terrainType);
    }

    // If this tile is owned by someone, fly their colors.
    if( spriteKey.commanderKey != null )
    {
      ss.colorize(defaultMapColors, getBuildingColors(spriteKey.commanderKey.myColor).paletteColors);
    }
    spriteSetMap.put(spriteKey, ss);
  }

  private static BufferedImage loadSpriteSheetFile(String filename)
  {
    BufferedImage bi = null;
    try
    {
      bi = ImageIO.read(new File(filename));
    }
    catch (IOException ioex)
    {
      System.out.println("WARNING! Exception loading resource file " + filename);
      bi = null;
    }
    return bi;
  }

  public static ImageFrame[] loadAnimation(String filename)
  {
    ImageFrame[] frames = null;
    try
    {
      ImageReader reader = (ImageReader) ImageIO.getImageReadersByFormatName("gif").next();
      ImageInputStream ciis = ImageIO.createImageInputStream(new File(filename));
      reader.setInput(ciis, false);

      frames = SpriteUIUtils.readGIF(reader, baseSpriteSize, baseSpriteSize);
    }
    catch (IOException ioex)
    {
      System.out.println("WARNING! Exception loading resource file " + filename);
      frames = null;
    }
    return frames;
  }

  public static Sprite getMoveCursorLineSprite()
  {
    if( moveCursorLineSprite == null )
    {
      moveCursorLineSprite = new Sprite(loadSpriteSheetFile("res/tileset/moveCursorLine.png"), baseSpriteSize, baseSpriteSize);
    }

    return moveCursorLineSprite;
  }

  public static Sprite getMoveCursorArrowSprite()
  {
    if( moveCursorArrowSprite == null )
    {
      moveCursorArrowSprite = new Sprite(loadSpriteSheetFile("res/tileset/moveCursorArrow.png"), baseSpriteSize, baseSpriteSize);
    }

    return moveCursorArrowSprite;
  }

  private static ColorPalette getBuildingColors(Color colorKey)
  {
    return buildingColorPalettes.get(colorKey);
  }

  private static ColorPalette getMapUnitColors(Color colorKey)
  {
    return mapUnitColorPalettes.get(colorKey);
  }

  private static class SpriteSetKey
  {
    public final TerrainType terrainKey;
    public final Commander commanderKey;
    private static ArrayList<SpriteSetKey> instances = new ArrayList<SpriteSetKey>();

    private SpriteSetKey(TerrainType terrain, Commander co)
    {
      terrainKey = terrain;
      commanderKey = co;
    }

    public static SpriteSetKey instance(TerrainType terrain, Commander co)
    {
      SpriteSetKey key = null;
      for( int i = 0; i < instances.size(); ++i )
      {
        if( instances.get(i).terrainKey == terrain && instances.get(i).commanderKey == co )
        {
          key = instances.get(i);
          break;
        }
      }
      if( key == null )
      {
        key = new SpriteSetKey(terrain, co);
        instances.add(key);
      }
      return key;
    }
  }

  ///////////////////////////////////////////////////////////////////
  //  Below is code for dealing with unit sprites.
  ///////////////////////////////////////////////////////////////////

  public static class UnitSpriteSetKey
  {
    public final UnitModel.UnitEnum unitTypeKey;
    public final Commander commanderKey;
    private static ArrayList<UnitSpriteSetKey> instances = new ArrayList<UnitSpriteSetKey>();

    private UnitSpriteSetKey(UnitModel.UnitEnum unitType, Commander co)
    {
      unitTypeKey = unitType;
      commanderKey = co;
    }

    public static UnitSpriteSetKey instance(UnitModel.UnitEnum unitType, Commander co)
    {
      UnitSpriteSetKey key = null;
      for( int i = 0; i < instances.size(); ++i )
      {
        if( instances.get(i).unitTypeKey == unitType && instances.get(i).commanderKey == co )
        {
          key = instances.get(i);
          break;
        }
      }
      if( key == null )
      {
        key = new UnitSpriteSetKey(unitType, co);
        instances.add(key);
      }
      return key;
    }
  }

  public static UnitSpriteSet getMapUnitSpriteSet(Unit unit)
  {
    UnitSpriteSetKey key = UnitSpriteSetKey.instance(unit.model.type, unit.CO);
    if( !mapUnitSpriteSetMap.containsKey(key) )
    {
      // We don't have it? Go load it.
      createMapUnitSpriteSet(key);
    }
    // We either found it or created it; it had better be there.
    return mapUnitSpriteSetMap.get(key);
  }

  public static void createMapUnitSpriteSet(UnitSpriteSetKey key)
  {
      String faction = key.commanderKey.factionName;
    System.out.println("creating " + key.unitTypeKey.toString() + " spriteset for CO " + key.commanderKey.myColor.toString());
    String filestr;
    UnitSpriteSet spriteSet;
    filestr = "res/unit/" + key.unitTypeKey.toString().replaceAll("\\_", "") + "_map.png";
    if( Commander.DEFAULT_SPRITE_KEY != faction )
    {
      filestr = ("res/unit/grey/" + faction + "/" + key.unitTypeKey.toString().toLowerCase()).replaceAll("\\_", "")+ "_map.png";
    }
    spriteSet = new UnitSpriteSet(loadSpriteSheetFile(filestr), baseSpriteSize, baseSpriteSize,
        getMapUnitColors(key.commanderKey.myColor));
    mapUnitSpriteSetMap.put(key, spriteSet);
  }

  /**
   * Code kinda-not-really stolen from https://stackoverflow.com/questions/20826216/copy-two-bufferedimages-into-one-image-side-by-side
   * join two BufferedImage
   * you can add a orientation parameter to control direction
   * you can use a array to join more BufferedImage
   */
  public static BufferedImage joinBufferedImage(ImageFrame[] frames, int w, int h, boolean flipImage)
  {
    //do some calculations first
    int offset = 0;
    int width = frames.length * (w + offset);
    int height = h;
    //create a new buffer and draw two image into the new image
    BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    Graphics g = newImage.getGraphics();
    for( int i = 0; i < frames.length; i++ )
    {
      if( flipImage )
        g.drawImage(frames[i].getImage(), w + i * (w + offset), 0, -w, h, null);
      else
        g.drawImage(frames[i].getImage(), i * (w + offset), 0, null);
    }
    return newImage;
  }
  
  public static double getGreyDiff(Color c)
  {
    int R = c.getRed();
    int G = c.getGreen();
    int B = c.getBlue();
    int total = R + G + B;
    double avg = total/3.0;
    double totalDiff = Math.abs(R-avg) +Math.abs(G-avg) +Math.abs(B-avg);
    return avg*100/(totalDiff+1);
    }
  
  // stolen from https://stackoverflow.com/questions/11456981/sorting-list-by-color-java
  public static double colorDistance(Color c1, Color c2) {
    double rmean = (c1.getRed() + c2.getRed()) / 2;
    int r = c1.getRed() - c2.getRed();
    int g = c1.getGreen() - c2.getGreen();
    int b = c1.getBlue() - c2.getBlue();
    double weightR = 2 + rmean / 256;
    double weightG = 4.0;
    double weightB = 2 + (255 - rmean) / 256;
    return Math.sqrt(weightR * r * r + weightG * g * g + weightB * b * b);
}

  public static BufferedImage createPaletteImage(Set<Color> paletteSet, int w, int h)
  {
    ArrayList<Color> palette = new ArrayList<Color>(paletteSet);

    // sort by greyness, so we cull the grayest things first
    palette.sort(new Comparator<Color>()
    {
      @Override
      public int compare(Color o1, Color o2)
      {
        double o1Diff = getGreyDiff(o1);
        double o2Diff = getGreyDiff(o2);
        return (int) (o1Diff - o2Diff);
      }
    });

    // Delete colors until we get to the proper number
    Color lastColor = Color.black;
    int diff = 1;
    int i = 0;
    while (palette.size() > 6)
    {
      for( ; i < palette.size(); i++ )
      {
        Color c = palette.get(i);
        if( diff < colorDistance(lastColor, c) )
        {
          lastColor = c;
        }
        else
        {
          palette.remove(i);
          break;
        }
      }
      if( i >= palette.size() )
      {
        i = 0;
        diff++;
      }
    }
    
    // sort by brightness
    palette.sort(new Comparator<Color>()
    {
      @Override
      public int compare(Color o1, Color o2)
      {
        return (int) (colorDistance(o1, Color.black) - colorDistance(o2, Color.black));
      }
    });
    
    int offset = 0;
    int width = palette.size() * (w + offset);
    int height = h;

    BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    Graphics g = newImage.getGraphics();
    
    for( int x = 0; x < palette.size(); x++ )
    {
      g.setColor(palette.get(x));
      g.fillRect(x * (w + offset), 0, w, h);
    }
    return newImage;
  }

  // Hunams
  static Color skinlight = new Color(248,216,128);
  static Color skinligh2 = new Color(251,219,133);
  static Color skindark = new Color(216,128,80);
  
  // Acid Rain
  static Color ARskinlight = new Color(248,216,128);
  
  // CI
  static Color beaklight = new Color(255,240,0);
  static Color beakdark = new Color(255,160,0);
  static Color beakdar2 = new Color(138,87,1);
  
  // BH
  static Color visor = new Color(168,216,224);
  
  // JS
  static Color plume1 = new Color(255,62,62);
  static Color plume2 = new Color(142,32,32);
  static Color plume3 = new Color(215,54,54);
  public static boolean shouldRecolor(Color tint)
  {
    int R = tint.getRed();
    int G = tint.getGreen();
    int B = tint.getBlue();
    if ( // if it's a banned color...
        tint.equals(skinlight) || 
        tint.equals(skinligh2) || 
        tint.equals(skindark) ||
        tint.equals(ARskinlight) || 
        tint.equals(beaklight) ||
        tint.equals(beakdark) ||
        tint.equals(beakdar2) ||
        tint.equals(visor) ||
        // or JS's plume
        tint.equals(plume1) ||
        tint.equals(plume2) ||
        tint.equals(plume3) ||
        // or transparent
        ((Long.MAX_VALUE & tint.getRGB()) <= 0) ||
        // or grey
        (
        Math.abs(R - G) < 20 &&
        Math.abs(R - B) < 20 &&
        Math.abs(G - B) < 20
        )
       ) // don't recolor it
      return false;
    return true;
  }
  
  public static void getColorInfo(ImageFrame[] frames, Map<Color,Integer> palette)
  {
    for( ImageFrame frame : frames )
    {
      BufferedImage bi = frame.getImage();
      for( int x = 0; x < bi.getWidth(); ++x )
      {
        for( int y = 0; y < bi.getHeight(); ++y )
        {
          Color tint = new Color(bi.getRGB(x, y));

          if( shouldRecolor(tint) )
          {
            if (null == palette.get(tint))
              palette.put(tint, 1);
            else
            palette.put(tint, palette.get(tint) + 1);
          }
        }
      }
    }
  }
  
  public static ImageFrame[] paintItGray(ImageFrame[] frames, Set<Color> palette)
  {
    int avgGrey = 0;
    for( Color g : defaultMapColors )
    {
      int R = g.getRed();
      int G = g.getGreen();
      int B = g.getBlue();
      avgGrey += (R + G + B) / 3;
    }
    avgGrey /= defaultMapColors.length;
    
    Map<ImageFrame, ArrayList<XYCoord>> imageRecolorInfo = new HashMap<>();
    ArrayList<Integer> avgBrightnesses = new ArrayList<>();
    
    for( ImageFrame frame : frames )
    {
      ArrayList<XYCoord> pixelsToRecolor = new ArrayList<>();
      int avgBrightnessTotal = 0; 
      
      BufferedImage bi = frame.getImage();
      for( int x = 0; x < bi.getWidth(); ++x )
      {
        for( int y = 0; y < bi.getHeight(); ++y )
        {
          Color tint = new Color(bi.getRGB(x, y));
          int R = tint.getRed();
          int G = tint.getGreen();
          int B = tint.getBlue();
          
          if( shouldRecolor(tint) )
          {
            pixelsToRecolor.add(new XYCoord(x, y));
            avgBrightnessTotal += (R + G + B)/3;
          }
        }
      }

      if( pixelsToRecolor.size() == 0 )
      {
        System.out.println("    no pixels to recolor");
      }
      else
      {
        int avgBrightness = avgBrightnessTotal / pixelsToRecolor.size();
        avgBrightnesses.add(avgBrightness);
        System.out.println("    Average frame brightness: " + avgBrightness);
      }
      imageRecolorInfo.put(frame, pixelsToRecolor);
    }

    int adjustment = 0, total = 0;
    for( int avg : avgBrightnesses )
    {
      total += avg;
    }
    if( avgBrightnesses.size() > 0 )
    {
      adjustment += avgGrey - (total / avgBrightnesses.size());
      System.out.println("    Adjusting frame brightness by: " + adjustment);
    }

    for (ImageFrame frame : imageRecolorInfo.keySet())
    {
      ArrayList<XYCoord> pixelsToRecolor = imageRecolorInfo.get(frame);
      BufferedImage bi = frame.getImage();
      
      for( XYCoord xyc : pixelsToRecolor )
      {
        Color tint = new Color(bi.getRGB(xyc.xCoord, xyc.yCoord));
        palette.add(tint);
        int R = tint.getRed();
        int G = tint.getGreen();
        int B = tint.getBlue();

        int adjustedHue = (R + G + B) / 3 + adjustment;
        int index = (int) (adjustedHue / (255.0 / defaultMapColors.length));
        if (index < 0)
          bi.setRGB(xyc.xCoord, xyc.yCoord, Color.black.getRGB());
        else if (index >= defaultMapColors.length - 1)
          bi.setRGB(xyc.xCoord, xyc.yCoord, Color.white.getRGB());
        else
          bi.setRGB(xyc.xCoord, xyc.yCoord, defaultMapColors[index].getRGB());
      }
    }
    return frames;
  }

  public static Sprite getMapUnitHPSprites()
  {
    if( null == mapUnitHPSprites )
    {
      mapUnitHPSprites = new Sprite(loadSpriteSheetFile("res/unit/icon/hp.png"), 8, 8);
    }
    return mapUnitHPSprites;
  }

  public static BufferedImage getCargoIcon()
  {
    if( null == mapUnitCargoIcon )
    {
      mapUnitCargoIcon = loadSpriteSheetFile("res/unit/icon/cargo.png");
    }
    return mapUnitCargoIcon;
  }

  public static BufferedImage getCaptureIcon()
  {
    if( null == mapUnitCaptureIcon )
    {
      mapUnitCaptureIcon = loadSpriteSheetFile("res/unit/icon/capture.png");
    }
    return mapUnitCaptureIcon;
  }

  ///////////////////////////////////////////////////////////////////
  //  Below is code for dealing with drawing in-game menus.
  ///////////////////////////////////////////////////////////////////

  /**
   * Returns a Sprite containing every uppercase letter, one letter per frame. The image
   * is loaded, and the Sprite is created, on the first call to this function, and simply
   * returned thereafter. Letters are stored in order starting from 'A'.
   * @return A Sprite object containing the in-game menu font for uppercase letters.
   */
  public static Sprite getLettersUppercase()
  {
    if( null == letterSpritesUppercase )
    {
      letterSpritesUppercase = new Sprite(loadSpriteSheetFile("res/ui/main/letters_uppercase.png"), 5, 11);
    }
    return letterSpritesUppercase;
  }

  /**
   * Returns a Sprite containing every lowercase letter, one letter per frame. The image
   * is loaded, and the Sprite is created, on the first call to this function, and simply
   * returned thereafter. Letters are stored in order starting from 'a'. 
   * @return A Sprite object containing the in-game menu font for lowercase letters.
   */
  public static Sprite getLettersLowercase()
  {
    if( null == letterSpritesLowercase )
    {
      letterSpritesLowercase = new Sprite(loadSpriteSheetFile("res/ui/main/letters_lowercase.png"), 5, 11);
    }
    return letterSpritesLowercase;
  }

  /**
   * Returns a Sprite containing the digits 0-9, one number per frame. The image
   * is loaded, and the Sprite is created, on the first call to this function, and simply
   * returned thereafter. Numbers are stored in order starting from 0.
   * @return A Sprite object containing the in-game menu font for numbers.
   */
  public static Sprite getNumbers()
  {
    if( null == numberSprites )
    {
      numberSprites = new Sprite(loadSpriteSheetFile("res/ui/main/numbers.png"), 5, 11);
    }
    return numberSprites;
  }

  /**
   * Returns a Sprite containing every small-uppercase letter, one letter per frame. The image
   * is loaded, and the Sprite is created, on the first call to this function, and simply
   * returned thereafter. Letters are all uppercase, and are stored in order starting from 'A'.
   * @return A Sprite object containing the in-game menu font for small-caps letters.
   */
  public static Sprite getLettersSmallCaps()
  {
    if( null == letterSpritesSmallCaps )
    {
      letterSpritesSmallCaps = new Sprite(loadSpriteSheetFile("res/ui/letters.png"), 5, 6);
    }
    return letterSpritesSmallCaps;
  }

  /**
   * This function returns numbers which match the font returned from getSmallLetters().
   * Returns a Sprite containing the digits 0-9, one number per frame. The image
   * is loaded, and the Sprite is created, on the first call to this function, and simply
   * returned thereafter. Numbers are stored in order starting from 0.
   * @return A Sprite object containing the in-game menu font for small-caps numbers.
   */
  public static Sprite getNumbersSmallCaps()
  {
    if( null == numberSpritesSmallCaps )
    {
      numberSpritesSmallCaps = new Sprite(loadSpriteSheetFile("res/ui/numbers.png"), 5, 6);
    }
    return numberSpritesSmallCaps;
  }

  /**
   * Draws the provided text at the provided location, using the standard alphanumeric sprite set.
   * @param g Graphics object to draw the text.
   * @param text Text to be drawn as sprited letters.
   * @param x X-coordinate of the top-left corner of the first letter to be drawn.
   * @param y Y-coordinate of the top-left corner of the first letter to be drawn.
   * @param scale Scaling factor to be applied when drawing.
   */
  public static void drawText(Graphics g, String text, int x, int y, int scale)
  {
    Sprite uppercase = getLettersUppercase();
    Sprite lowercase = getLettersLowercase();
    int menuTextWidth = uppercase.getFrame(0).getWidth() * scale;
    int menuTextHeight = uppercase.getFrame(0).getHeight() * scale;

    for( int i = 0; i < text.length(); ++i, x += menuTextWidth )
    {
      char thisChar = text.charAt(i);
      if( Character.isAlphabetic(thisChar) )
      {
        if( Character.isUpperCase(thisChar) )
        {
          int letterIndex = thisChar - 'A';
          g.drawImage(uppercase.getFrame(letterIndex), x, y, menuTextWidth, menuTextHeight, null);
        }
        else
        {
          int letterIndex = thisChar - 'a';
          g.drawImage(lowercase.getFrame(letterIndex), x, y, menuTextWidth, menuTextHeight, null);
        }
      }
      else if( Character.isDigit(thisChar) )
      {
        int letterIndex = thisChar - '0';
        g.drawImage(getNumbers().getFrame(letterIndex), x, y, menuTextWidth, menuTextHeight, null);
      }
    }
  }

  /**
   * Draws the provided text at the provided location, using the small-caps alphanumeric sprite set.
   * @param g Graphics object to draw the text.
   * @param text Text to be drawn as sprited letters.
   * @param x X-coordinate of the top-left corner of the first letter to be drawn.
   * @param y Y-coordinate of the top-left corner of the first letter to be drawn.
   * @param scale Scaling factor to be applied when drawing.
   */
  public static void drawTextSmallCaps(Graphics g, String text, int x, int y, int scale)
  {
    Sprite smallCaps = getLettersSmallCaps();
    int menuTextWidth = smallCaps.getFrame(0).getWidth() * scale;
    int menuTextHeight = smallCaps.getFrame(0).getHeight() * scale;
    text = text.toUpperCase(); // SmallCaps is all uppercase.

    for( int i = 0; i < text.length(); ++i, x += menuTextWidth )
    {
      if( Character.isAlphabetic(text.charAt(i)) )
      {
        int letterIndex = text.charAt(i) - 'A';
        g.drawImage(smallCaps.getFrame(letterIndex), x, y, menuTextWidth, menuTextHeight, null);
      }
      else if( Character.isDigit(text.charAt(i)) )
      {
        int letterIndex = text.charAt(i) - '0';
        g.drawImage(getNumbersSmallCaps().getFrame(letterIndex), x, y, menuTextWidth, menuTextHeight, null);
      }
    }
  }

  /**
   * Draws the provided image, centered around x, y.
   */
  public static void drawImageCenteredOnPoint(Graphics g, BufferedImage image, int x, int y, int drawScale)
  {
    // Calculate the size to draw.
    int drawWidth = image.getWidth() * drawScale;
    int drawHeight = image.getHeight() * drawScale;

    // Center over the target location.
    int drawX = x - drawWidth / 2;
    int drawY = y - drawHeight / 2;

    g.drawImage(image, drawX, drawY, drawWidth, drawHeight, null);
  }

  /**
   * Returns the overlay image for the HUD, which serves as a backdrop for the commander
   * name and the currently-available funds.
   * @param co The Commander whose overlay we are drawing. This allows us to colorize it appropriately.
   * @param leftSide Whether we want the overlay image for the left-side corner (false is right side).
   */
  public static BufferedImage getCoOverlay(Commander co, boolean leftSide)
  {
    if( !coOverlays.containsKey(co) )
    {
      final int OVERLAY_WIDTH = 97;
      final int OVERLAY_HEIGHT = 20;

      // If we don't already have this overlay, go load and store it.
      Sprite overlay = new Sprite(loadSpriteSheetFile("res/ui/co_overlay.png"), OVERLAY_WIDTH, OVERLAY_HEIGHT);
      overlay.colorize(defaultMapColors, mapUnitColorPalettes.get(co.myColor).paletteColors);

      // Draw the Commander's mug on top of the overlay.
      BufferedImage coMug = getCommanderSprites(co.coInfo.cmdrEnum).eyes;
      int mugW = coMug.getWidth();
      Graphics g = overlay.getFrame(0).getGraphics();
      g.drawImage(coMug, mugW, 1, -mugW, coMug.getHeight(), null);
      Graphics g1 = overlay.getFrame(1).getGraphics();
      g1.drawImage(coMug, OVERLAY_WIDTH - mugW, 1, null);

      coOverlays.put(co, overlay);
    }
    // Figure out which sub-image they want, and give it to them.
    int index = (leftSide) ? 0 : 1;
    return coOverlays.get(co).getFrame(index);
  }

  /** Draw and return an image with the CO's power bar. */
  public static BufferedImage getCoOverlayPowerBar(Commander co, int maxAP, double currentAP)
  {
    final int powerDrawScaleW = 2;
    BufferedImage bar = SpriteLibrary.createDefaultBlankSprite(maxAP * powerDrawScaleW, 5);
    Graphics barGfx = bar.getGraphics();

    // Get the CO's colors
    Color[] palette = SpriteLibrary.mapUnitColorPalettes.get(co.myColor).paletteColors;

    // Draw the bar
    barGfx.setColor(Color.BLACK); // Outside edge
    barGfx.drawRect(0, 0, bar.getWidth(), 4);
    barGfx.setColor(palette[5]); // Inside - empty
    barGfx.fillRect(0, 1, bar.getWidth(), 3);
    barGfx.setColor(palette[2]); // Inside - full
    barGfx.drawLine(0, 1, (int) (Math.floor(currentAP) * powerDrawScaleW), 1);
    barGfx.drawLine(0, 2, (int) (currentAP * powerDrawScaleW), 2);
    barGfx.drawLine(0, 3, (int) (Math.ceil(currentAP * powerDrawScaleW)), 3);

    return bar;
  }

  /** Return a Sprite with the various Ability Point images. */
  public static Sprite getCoOverlayPowerBarAPs(Commander co)
  {
    final int POWERBAR_FRAME_WIDTH = 7;
    final int POWERBAR_FRAME_HEIGHT = 9;

    // If we don't already have an image, load and colorize one for this Commander.
    if( !coPowerBarPieces.containsKey(co) )
    {
      // Image should be: empty bar, full bar, empty point, 1/3 point, 2/3 point, full point, large point.
      // Image should be: empty point, 1/3 point, 2/3 point, full point, large point.
      Sprite overlay = new Sprite(loadSpriteSheetFile("res/ui/powerbar_pieces.png"), POWERBAR_FRAME_WIDTH, POWERBAR_FRAME_HEIGHT);
      overlay.colorize(defaultMapColors, mapUnitColorPalettes.get(co.myColor).paletteColors);

      coPowerBarPieces.put(co, overlay);
    }
    return coPowerBarPieces.get(co);
  }

  /**
   * Creates a new blank (all black) image of the given size. This is used to generate placeholder
   * assets on the fly when we fail to load resources from disk.
   * @param w Desired width of the placeholder image.
   * @param h Desired height of the placeholder image.
   * @return A new BufferedImage of the specified size, filled in with all black.
   */
  public static BufferedImage createDefaultBlankSprite(int w, int h)
  {
    BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    Graphics big = bi.getGraphics();
    big.setColor(Color.BLACK);
    big.fillRect(0, 0, w, h);
    return bi;
  }

  /**
   * Creates a new transparent image of the given size. This is used as
   * a base for building generated image assets on the fly.
   * @param w Desired width of the image.
   * @param h Desired height of the image.
   * @return A new transparent BufferedImage of the specified size.
   */
  public static BufferedImage createTransparentSprite(int w, int h)
  {
    BufferedImage bi = createDefaultBlankSprite(w, h);
    Sprite spr = new Sprite(bi);
    spr.colorize(Color.BLACK, new Color(0, 0, 0, 0));
    return bi;
  }

  /**
   * If in is null, set it to a newly-generated blank image of default size. Return in.
   */
  public static BufferedImage createBlankImageIfNull(BufferedImage in)
  {
    if( null == in )
    {
      in = createDefaultBlankSprite(baseSpriteSize, baseSpriteSize);
    }
    return in;
  }

  /**
   * Get the image used to indicate the current action target.
   */
  public static BufferedImage getActionCursor()
  {
    if( null == actionCursor )
    {
      actionCursor = loadSpriteSheetFile("res/tileset/cursor_action.png");
      if( null == actionCursor )
      {
        actionCursor = createDefaultBlankSprite(baseSpriteSize, baseSpriteSize);
      }
    }
    return actionCursor;
  }

  /**
   * Returns the image text for the different options in the main menu, as frames in a Sprite.
   * The options shall be ordered within the Sprite to match the menuOptions array in MainController.
   * @return
   */
  public static Sprite getMainMenuOptions()
  {
    if( null == menuOptionsSprite )
    {
      menuOptionsSprite = new Sprite(createBlankImageIfNull(loadSpriteSheetFile("res/ui/main/newgame.png")));
      menuOptionsSprite.addFrame(createBlankImageIfNull(loadSpriteSheetFile("res/ui/main/options.png")));
      menuOptionsSprite.addFrame(createBlankImageIfNull(loadSpriteSheetFile("res/ui/main/quit.png")));
    }
    return menuOptionsSprite;
  }

  public static BufferedImage getGameOverDefeatText()
  {
    if( null == gameOverDefeatText )
    {
      gameOverDefeatText = loadSpriteSheetFile("res/ui/defeat.png");
    }
    return gameOverDefeatText;
  }

  public static BufferedImage getGameOverVictoryText()
  {
    if( null == gameOverVictoryText )
    {
      gameOverVictoryText = loadSpriteSheetFile("res/ui/victory.png");
    }
    return gameOverVictoryText;
  }
  ///////////////////////////////////////////////////////////////////
  //  Below is code for loading Commander sprite images.
  ///////////////////////////////////////////////////////////////////

  private static HashMap<CommanderLibrary.CommanderEnum, CommanderSpriteSet> coSpriteSets = new HashMap<CommanderLibrary.CommanderEnum, CommanderSpriteSet>();

  public static CommanderSpriteSet getCommanderSprites(CommanderLibrary.CommanderEnum whichCo)
  {
    CommanderSpriteSet css = null;

    if( !coSpriteSets.containsKey(whichCo) )
    {
      // We don't have it, so we need to load it.
      String baseFileName = getCommanderBaseSpriteName(whichCo);

      BufferedImage body = createBlankImageIfNull(loadSpriteSheetFile(baseFileName + ".png"));
      BufferedImage head = createBlankImageIfNull(loadSpriteSheetFile(baseFileName + "_face.png"));
      BufferedImage eyes = createBlankImageIfNull(loadSpriteSheetFile(baseFileName + "_eyes.png"));

      coSpriteSets.put(whichCo, new CommanderSpriteSet(body, head, eyes));
    }

    css = coSpriteSets.get(whichCo);

    return css;
  }

  private static String getCommanderBaseSpriteName(CommanderLibrary.CommanderEnum whichCo)
  {
    String str = "res/co/";
    switch (whichCo)
    {
      case LION:
        str += "lion";
        break;
      case PATCH:
        str += "patch";
        break;
      case STRONG:
        str += "strong";
        break;
      case NOONE:
      default:
        // Not a real Commander. Gonna fall back to placeholder images.
    }
    return str;
  }
}
