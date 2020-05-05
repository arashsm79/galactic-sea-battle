package ship;
public class Ship {

     private int areaRow;
     private int areaCol;

     private int power;

     private int moveLimit;

     private int price;
     Ship(int areaRow, int areaCol, int power, int moveLimit, int price)
     {
        this.areaRow = areaRow;
        this.areaCol = areaCol;
        this.power = power;
        this.moveLimit = moveLimit;
        this.price = price;
     }

     /**
      * @param areaCol the areaCol to set
      */
     public void setAreaCol(int areaCol) {
         this.areaCol = areaCol;
     }
     /**
      * @return the areaCol
      */
     public int getAreaCol() {
         return areaCol;
     }

     /**
      * @param areaRow the areaRow to set
      */
     public void setAreaRow(int areaRow) {
         this.areaRow = areaRow;
     }
     /**
      * @return the areaRow
      */
     public int getAreaRow() {
         return areaRow;
     }
     /**
      * @param power the power to set
      */
     public void setPower(int power) {
         this.power = power;
     }
     /**
      * @return the power
      */
     public int getPower() {
         return power;
     }
     /**
      * @param moveLimit the moveLimit to set
      */
     public void setMoveLimit(int moveLimit) {
         this.moveLimit = moveLimit;
     }
     /**
      * @return the moveLimit
      */
     public int getMoveLimit() {
         return moveLimit;
     }
     /**
      * @param price the price to set
      */
     public void setPrice(int price) {
         this.price = price;
     }
     /**
      * @return the price
      */
     public int getPrice() {
         return price;
     }

}