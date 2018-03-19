package com.redhat.summit2018;

public class Label
{
   private String voc;
   private float score;

   public Label()
   {
   }

   public Label(String voc, float score)
   {
      this.voc = voc;
      this.score = score;
   }

   public void setVoc(String voc)
   {
      this.voc = voc;
   }

   public String getVoc()
   {
      return voc;
   }

   public void setScore(float score)
   {
      this.score = score;
   }

   public float getScore()
   {
      return score;
   }
}
