package com.redhat.summit2018;

public class Label
{
   private String voc;
   private float score;
   private float tl_x;
   private float tl_y;
   private float br_x;
   private float br_y;

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

   public void setTl_x(float tl_x)
   {
      this.tl_x = tl_x;
   }

   public float getTl_x()
   {
      return tl_x;
   }

   public void setTl_y(float tl_y)
   {
      this.tl_y = tl_y;
   }

   public float getTl_y()
   {
      return tl_y;
   }

   public void setBr_x(float br_x)
   {
      this.br_x = br_x;
   }

   public float getBr_x()
   {
      return br_x;
   }

   public void setBr_y(float br_y)
   {
      this.br_y = br_y;
   }

   public float getBr_y()
   {
      return br_y;
   }

   public float getArea()
   {
      return (br_x - tl_x) * (br_y - tl_y);
   }
}
