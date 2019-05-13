package com.romanyu.chat.gallery;

public class ImageData {
    private static int currentNumber = 1;
    private String imagePath;
    private boolean isCheck;
    private int checkNumber;

    public ImageData(String imagePath){
        this.imagePath = imagePath;
        this.isCheck = false;
    }

    public static void resetNumber(){
        currentNumber = 1;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public boolean isCheck() {
        return isCheck;
    }

    public void setCheck(boolean check) {
        if(isCheck == check){return;}
        if(check) {
            checkNumber = currentNumber;
            currentNumber++;
        }else{
            currentNumber--;
        }
        isCheck = check;
    }

    public int getCheckNumber() {
        return checkNumber;
    }

    public void setCheckNumber(int checkNumber) {
        this.checkNumber = checkNumber;
    }
}
