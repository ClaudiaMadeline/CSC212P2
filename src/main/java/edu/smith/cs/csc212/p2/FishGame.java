package edu.smith.cs.csc212.p2;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * This class manages our model of gameplay: missing and found fish, etc.
 * @author jfoley
 *
 */
public class FishGame {
	/*
	 * Random in case you want random numbers!
	 */
	Random rand = ThreadLocalRandom.current();
	/**
	 * This is the world in which the fish are missing. (It's mostly a List!).
	 */
	World world;
	/**
	 * The player (a Fish.COLORS[0]-colored fish) goes seeking their friends.
	 */
	Fish player;
	/**
	 * The home location.
	 */
	FishHome home;
	/**
	 * These are the missing fish!
	 */
	List<Fish> missing;
	
	/**
	 * These are fish we've found!
	 */
	List<Fish> found;
	
	/**
	 * These are fish that came home!
	 */
	List<Fish> fish_home;
	
	/**
	 * Number of steps!
	 */
	int stepsTaken;
	/**
	 * Number of steps it takes for fish to wander from line.
	 */
	int wanderSteps;
	
	/**
	 * Score!
	 */
	int score;
	
	/**
	 * Create a FishGame of a particular size.
	 * @param w how wide is the grid?
	 * @param h how tall is the grid?
	 */
	public FishGame(int w, int h) {
		world = new World(w, h);
		
		missing = new ArrayList<Fish>();
		found = new ArrayList<Fish>();
		fish_home = new ArrayList<Fish>();
		
		// Add a home!
		home = world.insertFishHome();
		
		// Add regular rocks!
		final int NUM_ROCKS = 10;

		for (int i=0; i<NUM_ROCKS; i++) 
		{
			world.insertRockRandomly();
		}
		
		// Add falling rocks!
		final int NUM_FALLING_ROCKS = 5;

		for (int i=0; i<NUM_FALLING_ROCKS; i++) 
		{
			world.insertFallingRockRandomly();
		}
		
		// Add a snail!
		world.insertSnailRandomly();
		
		// Make the player out of the 0th fish color.
		player = new Fish(0, world);
		// Start the player at "home".
		player.setPosition(home.getX(), home.getY());
		player.markAsPlayer();
		world.register(player);
		
		// Generate fish of all the colors but the first into the "missing" List.
		for (int ft = 1; ft < Fish.COLORS.length; ft++) {
			Fish friend = world.insertFishRandomly(ft);
			missing.add(friend);
		}
	}
	
	
	/**
	 * How we tell if the game is over: if missingFishLeft() == 0.
	 * @return the size of the missing list.
	 */
	public int missingFishLeft() {
		return missing.size();
	}
	
	/**
	 * This method is how the PlayFish app tells whether we're done.
	 * @return true if the player has won (or maybe lost?).
	 */
	public boolean gameOver() {
		return fish_home.size()==7;
	}

	/**
	 * Update positions of everything (the user has just pressed a button).
	 */
	public void step() {
		// Keep track of how long the game has run.
		this.stepsTaken += 1;
		this.wanderSteps++;
		// These are all the objects in the world in the same cell as the player.
		List<WorldObject> overlap = this.player.findSameCell();
		// The player is there, too, let's skip them.
		overlap.remove(this.player);
		
		// If we find a fish, remove it from missing.
		for (WorldObject wo : overlap) {
			// It is missing if it's in our missing list.
			if (missing.contains(wo)) {
				// Remove this fish from the missing list.
				missing.remove(wo);
				
				// Add to list of found fish
				found.add((Fish)wo);
				
				// Increase score when you find a fish!
				score += found.get(found.size()-1).points;
			}
		}
		// If a fish gets home, remove it from found.
		for(int i=0; i<found.size(); i++)
		{
			Fish home_fish = found.get(i);
			// These are all the objects in the world in the same cell as home_fish.
			List<WorldObject> fish_overlap = home_fish.findSameCell();
			// The home_fish is there, so it should be skipped.
			fish_overlap.remove(home_fish);
			
			// If the fish overlaps with home, remove the fish from found
			for (WorldObject wo : fish_overlap) 
			{
				if(wo.equals(home))
				{
					// Remove the fish from the found list
					found.remove(home_fish);
					// Remove the fish from the world
					world.remove(home_fish);
					// Add the fish to the fish_home list
					fish_home.add(home_fish);
				}
			}
		}
		
		// If all the follower fish went home, the player should be removed once it comes home.
		if(missing.size()==0 && found.size()==0)
		{
			// If the player overlaps with home, remove it from the world and add it to fish_home.
			for (WorldObject wo : overlap) {
				if(wo.equals(home))
				{
					fish_home.add(this.player);
					world.remove(this.player);
				}
			}
		}
		
		// Every 20 steps, there is a chance some fish can wander
		if(wanderSteps > 20)
		{
			// Fish further than two from the front have a chance of wandering
			for(int i=2; i<found.size(); i++)
			{
				Fish fishie = found.get(i);
				if(rand.nextInt(10)>6)
					found.remove(fishie);
					missing.add(fishie);
			}
			// wanderStep resets so it will take 20 more steps for fish to have the chance of wandering.
			wanderSteps = 0;
		}
		
		// Make sure missing fish *do* something.
		wanderMissingFish();
		// When fish get added to "found" they will follow the player around.
		World.objectsFollow(player, found);
		// Step any world-objects that run themselves.
		world.stepAll();
	}
	
	/**
	 * Call moveRandomly() on all of the missing fish to make them seem alive.
	 */
	private void wanderMissingFish() {
		Random rand = ThreadLocalRandom.current();
		for (Fish lost : missing) 
		{
			if(!lost.fastScared)
			{
				// 30% of the time, lost fish move randomly.
				if (rand.nextDouble() < 0.3) 
				{
					lost.moveRandomly();
				}
			}
			else
			{
				// 80% of the time, fastScared lost fish move randomly.
				if (rand.nextDouble() < 0.8) 
				{
					lost.moveRandomly();
				}
			}
		}
	}

	/**
	 * This gets a click on the grid. We want it to destroy rocks that ruin the game.
	 * @param x - the x-tile.
	 * @param y - the y-tile.
	 */
	public void click(int x, int y) {
		System.out.println("Clicked on: "+x+","+y+ " world.canSwim(player,...)="+world.canSwim(player, x, y));
		List<WorldObject> atPoint = world.find(x, y);
		for(int i=0; i<atPoint.size(); i++)
		{
			List<WorldObject> sameCell = atPoint.get(i).findSameCell();
			for(int j=0; j<sameCell.size(); j++)
			{
				if(atPoint.get(i).inSameSpot(sameCell.get(j)))
				{
					sameCell.get(j).remove();
				}
			}
		}

	}
	
}
