package model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import control.EstimatorInterface;

public class DummyLocalizer implements EstimatorInterface {
		
	private int rows, cols, head, posX, posY;
	Random rand = new Random();
	int[] xDir = {-1,0,1,0}; // UP,RIGHT,DOWN,LEFT
	int[] yDir = {0,1,0,-1};
	double[][] fMatrix;

	public DummyLocalizer( int rows, int cols, int head) {
		this.rows = rows;
		this.cols = cols;
		this.head = head;		
		this.posX = rand.nextInt(rows);
		this.posY = rand.nextInt(cols);
		this.fMatrix = initializeFMatrix(); 
	}	
	
	public int getNumRows() {
		return rows;
	}
	
	public int getNumCols() {
		return cols;
	}
	
	public int getNumHead() {
		return head;
	}
	
	public double getTProb( int x, int y, int h, int nX, int nY, int nH) {
		if (!isAdjacent(x,y,nX,nY) || isOutside(nX,nY) || isOutside(x,y))
			return 0.0;
		// same direction
		if (h == nH && nX == x+xDir[h] && nY == y+yDir[h])
			return 0.7;
		if (h == nH)
			return 0.0;
		// different direction
		double total_probs = 0.3;
		if (isFacingWall(x,y,h))
			total_probs = 1.0;
		int count = 0;
		for (int i=0; i<4; i++) 
			if (i!=h && !isOutside(x+xDir[i],y+yDir[i]))
				count++;
		for (int i=0; i<4; i++) 
			if (i==nH && nX==x+xDir[i] && nY==y+yDir[i])
				return total_probs/count;
		return 0.0;
	}

	public double getOrXY( int rX, int rY, int x, int y) {
		List<ArrayList<Integer>> firstField = possibleLoc1(x,y);
		for (ArrayList<Integer> pos: firstField) {
			if (rX == pos.get(0) && rY == pos.get(1))
				return 0.05;
		}
		List<ArrayList<Integer>> secondField = possibleLoc2(x,y);
		for (ArrayList<Integer> pos: secondField) {
			if (rX == pos.get(0) && rY == pos.get(1))
				return 0.025;
		}
		if (x==rX && y==rY)
			return 0.1;
		else if(rX==-1 ||rY==-1)
			return 1 - 0.1 - 0.05*firstField.size() - 0.025*secondField.size();
		return 0.0;
	}


	public int[] getCurrentTruePosition() {
		int[] ret = new int[2];
		
		if(isFacingWall(posX,posY,head)){
			int dirIndex = randDirection(); 
			ret[0] = posX + xDir[dirIndex];
			ret[1] = posY + yDir[dirIndex];
		} else {
			double prob = rand.nextDouble();
			if (prob<=0.3) {
				int dirIndex = randDirection(); 
				ret[0] = posX + xDir[dirIndex];
				ret[1] = posY + yDir[dirIndex];
			} else {
				ret[0] = posX + xDir[head];
				ret[1] = posY + yDir[head];
			}
		}
		posX = ret[0];
		posY = ret[1];
		return ret;
	}

	public int[] getCurrentReading() {
		int[] ret = new int[2];
		double prob = rand.nextDouble();
		
		if(prob <= 0.1){
			ret[0] = posX;
			ret[1] = posY;
		}else if(prob <= 0.1 + 8 * 0.05){
			ret = getRandomLoc1();
		}else if(prob <= 0.1 + 8 * 0.05 + 16 * 0.025)
			ret = getRandomLoc2();
		else
			ret = null;
		return ret;
	}


	public double getCurrentProb( int x, int y) {
		double ret = 0.0;
		for (int i=0; i<4; i++) {
			ret += fMatrix[x*cols*4+y*cols+i][0];
		}
		return ret;
	}
	
	public void update() {
		int[] senseLocation= getCurrentReading();
		double[][] oMatrix = getOMatrix(senseLocation);
		double[][] tMatrix = getTMatrix();
		//forward step
		double[][]temp = multiplyMatrix(oMatrix,tranMatrix(tMatrix,rows*cols*4,rows*cols*4),rows*cols*4,rows*cols*4,rows*cols*4,rows*cols*4);
		fMatrix = multiplyMatrix(temp,fMatrix,rows*cols*4,rows*cols*4,rows*cols*4,1);
		double sum=0.0;
		for (int i=0; i<rows*cols*4; i++)
			sum += fMatrix[i][0];
		for (int i=0; i<rows*cols*4; i++)
			fMatrix[i][0]/=sum;
		
		//report
		double maxProb = -1.0;
		int[] bestPos = new int[2];
		for (int i=0; i<rows; i++) for(int j=0; j<cols; j++) {
			double p = getCurrentProb(i,j);
			if (p>maxProb) {
				maxProb = p;
				bestPos[0] = i;
				bestPos[1] = j;
			}
		}
		System.out.printf("Current true position is: (%d,%d)\n", posX,posY);
		System.out.printf("Current predicted position is: (%d,%d)\n", bestPos[0],bestPos[1]);
		System.out.printf("Manhattan distance: %d\n\n", Math.abs(bestPos[0]-posX) + Math.abs(bestPos[1]-posY));
		
	}
	
	public int[] getRandomLoc1(){
		int [] ret = {-1,-1};
		List<ArrayList<Integer> > firstField = new ArrayList<ArrayList<Integer>>();
		
		int[] xDiff = {-1,-1,-1,0,0,1,1,1};
		int[] yDiff = {-1,0,1,-1,1,-1,0,1};
		
		for (int i=0; i<8; i++) {
			ArrayList<Integer> adjPos = new ArrayList<Integer>();
			adjPos.add(posX+xDiff[i]);
			adjPos.add(posY+yDiff[i]);
			firstField.add(adjPos);
		}
		
		int randIndex = rand.nextInt(8);
		
		ArrayList<Integer> selected = firstField.get(randIndex);
		int xSelected = selected.get(0);
		int ySelected = selected.get(1);
		
		if(isOutside(xSelected,ySelected))
			return null;
		
		ret[0] = xSelected;
		ret[1] = ySelected;
		
		return ret;
	}
	
	public int[] getRandomLoc2(){
		int [] ret = {-1,-1};
		List<ArrayList<Integer> > secondField = new ArrayList<ArrayList<Integer>>();
		
		int[] xDiff = {-2,-2,-2,-2,-2,-1,-1,0,0,1,1,2,2,2,2,2};
		int[] yDiff = {-2,-1,0,1,2,-2,2,-2,2,-2,2,-2,-1,0,1,2};
		
		for (int i=0; i<16; i++) {
			ArrayList<Integer> adjPos = new ArrayList<Integer>();
			adjPos.add(posX+xDiff[i]);
			adjPos.add(posY+yDiff[i]);
			secondField.add(adjPos);
		}
		
		int randIndex = rand.nextInt(16);
		
		ArrayList<Integer> selected = secondField.get(randIndex);
		int xSelected = selected.get(0);
		int ySelected = selected.get(1);
		
		if(isOutside(xSelected,ySelected))
			return null;
		
		ret[0] = xSelected;
		ret[1] = ySelected;
		
		return ret;
	}
	
	// Hashing scheme: state (x,y,dir) -> x*cols*4 + y*4 + dir
	public double[][] getTMatrix() {
		double[][] tMatrix = new double[rows*cols*4][rows*cols*4];
		for (int x=0; x<rows; x++) for (int y=0; y<cols; y++) for (int h=0; h<4; h++) {
			int index = x*cols*4 + y*4 + h;
			for (int dir=0; dir<4; dir++) {
				int nX = x + xDir[dir];
				int nY = y + yDir[dir];
				if (isOutside(nX,nY)) 
					continue;
				for (int nH=0; nH<4; nH++) {
					int nIndex = nX*cols*4 + nY*4 + nH;
					tMatrix[index][nIndex] = getTProb(x,y,h,nX,nY,nH);
				}
			}
		}
		return tMatrix;
	}
	
	public double[][] getOMatrix(int[] coord){
		if(coord == null)
            return getNullMatrix();
		
		int x= coord[0];
		int y= coord[1];
		double[][] matrix = new double[rows*cols*4][rows*cols*4];
		
		int index = x * cols * 4 + y * 4;
		
        for(int i = 0; i < 0 ; i++){
        	matrix[index + i][index + i] = 0.1;
        }
        setProbNeighbor(matrix, possibleLoc1(x, y), 0.05);
        setProbNeighbor(matrix, possibleLoc2(x, y), 0.025);
		return matrix;
	}
	
	public double[][] getNullMatrix(){
		double[][] matrix = new double[rows*cols*4][rows*cols*4];
		
		for(int i = 0;i < rows*cols*4; i++) for(int j = 0; j < rows*cols*4; j++){
			matrix[i][j] = 0;
		}
		
		for(int i = 0; i < cols * rows * 4; i++){
            int posX = i / (cols * 4);
            int posY = (i / 4) % cols;

            int prob1 = 8 - possibleLoc1(posX, posY).size();
            int prob2 = 16 - possibleLoc2(posX, posY).size();
            
            matrix[i][i] = 0.1 +  prob1 * 0.05 + prob2 * 0.025;
		}
		
		return matrix;
	}

	public void setProbNeighbor(double[][] matrix, List<ArrayList<Integer> > possibleLoc2, double prob){
		for(ArrayList<Integer> element: possibleLoc2){
			int ind = element.get(0) * cols  * 4 + element.get(1) * 4;
			for(int i = 0; i < 4; i++){
				matrix[ind + i][ind + i] = prob;
			}
		}
	}	
	
	public List<ArrayList<Integer> > possibleLoc1(int posX, int posY){
		List<ArrayList<Integer> > firstField = new ArrayList<ArrayList<Integer>>();
		
		int[] xDiff = {-1,-1,-1,0,0,1,1,1};
		int[] yDiff = {-1,0,1,-1,1,-1,0,1};
		
		for (int i=0; i<8; i++) {
			ArrayList<Integer> adjPos = new ArrayList<Integer>();
			int xSelected = posX+xDiff[i];
			int ySelected = posY+yDiff[i];
			adjPos.add(xSelected);
			adjPos.add(ySelected);
			
			if(!isOutside(xSelected,ySelected))
				firstField.add(adjPos);
				
		}   
        return firstField;    
		
	}
	
	public List<ArrayList<Integer> > possibleLoc2(int posX, int posY){
		List<ArrayList<Integer> > secondField = new ArrayList<ArrayList<Integer>>();
		
		int[] xDiff = {-2,-2,-2,-2,-2,-1,-1,0,0,1,1,2,2,2,2,2};
		int[] yDiff = {-2,-1,0,1,2,-2,2,-2,2,-2,2,-2,-1,0,1,2};
		
		for (int i=0; i<16; i++) {
			ArrayList<Integer> adjPos = new ArrayList<Integer>();
			int xSelected = posX+xDiff[i];
			int ySelected = posY+yDiff[i];
			adjPos.add(xSelected);
			adjPos.add(ySelected);
			
			if(!isOutside(xSelected,ySelected))
				secondField.add(adjPos);
		}
		
		return secondField;
	}

	public boolean isFacingWall(int x,int y, int h) {
		int newX = x + xDir[h];
		int newY = y + yDir[h];
		return isOutside(newX, newY);
	}

	public boolean isInCorner(int x, int y) {
		if (x!=0 || x!=rows-1 || y!=0 || y!=cols-1)
			return false;
		if (x==0) 
			return y==0 || y==cols-1;
		return x == 0 || x==rows-1;
	}
	
	public boolean isOutside(int xCoord, int yCoord) {
		return xCoord < 0 || xCoord >= rows || yCoord < 0 || yCoord >= cols;
	}
	
	public boolean isAdjacent(int x, int y, int nX, int nY) {
		if (x==nX)
			return y==nY-1 || y==nY+1;
		if (y==nY)
			return x==nX-1 || y==nY+1;
		return false;
	}
	
	public double[][] multiplyMatrix(double[][] matrix1, double[][] matrix2, int rows1,  int cols1, int rows2, int cols2) {
		if (cols1!=rows2)
			return null;
		double[][] result = new double[rows1][cols2];
		for (int i=0; i<rows1; i++) for (int j=0; j<cols2; j++) {
			double ans = 0.0;
			for (int k=0; k<cols1; k++) 
				ans += matrix1[i][k] * matrix2[k][j];
			result[i][j] = ans;
		}
		return result;
	}
	
	public double[][] tranMatrix(double[][] matrix, int row, int col) {
		double[][] ret = new double[col][row];
		for (int i=0; i<row; i++) for (int j=0; j<col;j++) {
			ret[i][j] = matrix[j][i];
		}
		return ret;
	}
	
	public int randDirection(){
		ArrayList<Integer> possibleDir = new ArrayList<Integer>();
		for (int i=0; i<4; i++)
			if (i!=head && !isOutside(posX+xDir[i],posY+yDir[i]))
				possibleDir.add(i);
		int randIndex = rand.nextInt(possibleDir.size());
		return possibleDir.get(randIndex);
	}
	
	public double[][] initializeFMatrix(){
		double[][] fMatrix = new double[rows*cols*4][1];
		
		for(int i = 0;i < rows*cols*4; i++){
			fMatrix[i][0] = 1.0 / (rows*cols*4);
		}
		return fMatrix;
	}
	
}