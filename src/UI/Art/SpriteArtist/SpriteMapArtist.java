package UI.Art.SpriteArtist;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

import Terrain.Environment;
import Terrain.GameMap;
import UI.MapView;
import UI.Art.MapArtist;
import UI.Art.FillRectArtist.FillRectMapArtist;

import Engine.GameInstance;
import Engine.Path;

public class SpriteMapArtist implements MapArtist
{
	private GameInstance myGame;
	private GameMap gameMap;
	
	BufferedImage baseMapImage;

	MapArtist backupArtist; // TODO: Make this obsolete.
	
	public SpriteMapArtist(GameInstance game)
	{
		myGame = game;
		gameMap = game.gameMap;
		
		backupArtist = new FillRectMapArtist(myGame);
		
		baseMapImage = new BufferedImage(gameMap.mapWidth*MapView.getTileSize(), 
				gameMap.mapHeight*MapView.getTileSize(), BufferedImage.TYPE_INT_RGB);
		
		// Build base map image.
		buildMapImage(gameMap);
	}
	@Override
	public void drawMap(Graphics g)
	{
		// TODO: Change what/where we draw based on camera location.
		g.drawImage(baseMapImage, 0, 0, baseMapImage.getWidth()*SpriteLibrary.drawScale, baseMapImage.getHeight()*SpriteLibrary.drawScale, null);
		g.drawImage(baseMapImage, 0, 0, null);
	}

	@Override
	public void drawCursor(Graphics g)
	{
		backupArtist.drawCursor(g);
	}

	@Override
	public void drawMovePath(Graphics g, Path p)
	{
		backupArtist.drawMovePath(g, p);
	}

	@Override
	public void drawHighlights(Graphics g)
	{
		backupArtist.drawHighlights(g);
	}
	
	/**
	 * Determines the base terrain type for the provided environment terrain type.
	 * For example, FOREST is a tile type, but the trees sit on a plain, so for drawing
	 * purposes (esp. terrain transitions), the base tile type of FOREST is actually PLAIN. 
	 */
	private Environment.Terrains getBaseTerrainType(Environment.Terrains terrainType)
	{
		Environment.Terrains baseTerrain = terrainType;
		switch( baseTerrain )
		{
		case CITY:
		case DUNES:
		case FACTORY:
		case FOREST:
		case HQ:
		case MOUNTAIN:
		case GRASS:
		case ROAD:
			baseTerrain = Environment.Terrains.GRASS;
			break;
		case SHOAL:
			baseTerrain = Environment.Terrains.SHOAL;
			break;
		case REEF:
		case OCEAN:
			baseTerrain = Environment.Terrains.OCEAN;
			break;
			default:
				System.out.println("ERROR! [SpriteMapArtist.buildMapImage] Invalid terrain type " + baseTerrain);
		}
		
		return baseTerrain;
	}
	
	private void buildMapImage(GameMap map)
	{
		Graphics g = baseMapImage.getGraphics();
		
		// Choose and draw all base sprites (grass, water, shallows).
		for(int x = 0; x < map.mapWidth; ++x)
		{
			for(int y = 0; y < map.mapHeight; ++y)
			{
				Environment.Terrains tileTerrain = map.getLocation(x, y).getEnvironment().terrainType;
				
				tileTerrain = getBaseTerrainType(tileTerrain);
				
				TerrainSpriteSet spriteSet = SpriteLibrary.getTerrainSpriteSet( tileTerrain );
				
				spriteSet.drawTile(g, gameMap, x*MapView.getTileSize(), y*MapView.getTileSize(), SpriteLibrary.drawScale);
				//g.drawImage(spriteSet.chooseTileImage(gameMap, x, y), x*SpriteLibrary.spriteSize, y*SpriteLibrary.spriteSize, null);
			}
		}
		
		// Draw all map object sprites (e.g. trees, mountains, buildings).
	}
}
