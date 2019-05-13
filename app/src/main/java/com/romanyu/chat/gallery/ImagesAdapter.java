package com.romanyu.chat.gallery;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.romanyu.chat.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ImagesAdapter extends RecyclerView.Adapter<ImagesAdapter.ImageHolder> {

    public interface IShowableSendButton{
        void showButton();
        void hideButton();
    }

    private Context context;
    private List<ImageData> imagesList;
    private List<ImageData> selectImages = new ArrayList<>();
    private IShowableSendButton iShowableSendButton;

    public ImagesAdapter(Context context, List<ImageData> imagesList){
        this.context = context;
        this.imagesList = imagesList;
        this.iShowableSendButton = (IShowableSendButton)context;
    }

    @NonNull
    @Override
    public ImageHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
           View view = LayoutInflater.from(context).inflate(R.layout.image_item, viewGroup, false);
           return new ImageHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageHolder imageHolder, int position) {
        ImageData imageData = imagesList.get(position);
        Glide.with(context)
                .load(Uri.fromFile(new File(imageData.getImagePath())))
                .into(imageHolder.imageView);
        if(imageData.isCheck()){
            imageHolder.setImageCircleAsUnvisible();
            imageHolder.setSelectImageCircleAsChecked(imageData.getCheckNumber());
        }else{
            imageHolder.setImageCircleAsVisible();
            imageHolder.setSelectImageCircleAsUnchecked();
        }
    }

    @Override
    public int getItemCount() {
        return imagesList.size();
    }

    private void rewriteHoldersDatas(){
        int num = 1;
        for(ImageData imageData : selectImages){
            imageData.setCheckNumber(num);
            num++;
        }
        notifyDataSetChanged();
    }

    class ImageHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        private ImageView imageView;
        private View imageCircle;
        private TextView selectImageCircle;

        ImageHolder(View view){
            super(view);
            imageView = (ImageView) view.findViewById(R.id.image_view);
            imageCircle = view.findViewById(R.id.image_circle);
            imageCircle.setOnClickListener(this);
            selectImageCircle = (TextView)view.findViewById(R.id.select_image_circle);
            selectImageCircle.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();
            switch (v.getId()){
                case R.id.image_circle: {
                    onClickImageCircle(position);
                    break;
                }
                case R.id.select_image_circle:{
                    onClickSelectImageCircle(position);
                    break;
                }
            }
        }

        private void onClickImageCircle(int position){
            ImageData imageData = imagesList.get(position);
            imageData.setCheck(true);
            setSelectImageCircleAsChecked(imageData.getCheckNumber());
            selectImages.add(imageData);
            Animation hideAnimation = AnimationUtils.loadAnimation(context, R.anim.image_circle_anim_to_hide);
            hideAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    setImageCircleAsUnvisible();
                    checkAndShowOrHideSendImageButton();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            imageCircle.startAnimation(hideAnimation);
        }

        private void onClickSelectImageCircle(int position){
            ImageData imageData = imagesList.get(position);
            imageData.setCheck(false);
            setImageCircleAsVisible();
            selectImages.remove(imageData);
            Animation showAnimation = AnimationUtils.loadAnimation(context, R.anim.image_circle_anim_to_show);
            showAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    setSelectImageCircleAsUnchecked();
                    rewriteHoldersDatas();
                    checkAndShowOrHideSendImageButton();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            imageCircle.startAnimation(showAnimation);
        }

        private void setSelectImageCircleAsChecked(int checkNumber){
            selectImageCircle.setVisibility(View.VISIBLE);
            selectImageCircle.setText(Integer.toString(checkNumber));
            imageCircle.setBackgroundResource(R.drawable.image_circle_fill);
        }

        private void setSelectImageCircleAsUnchecked(){
            selectImageCircle.setVisibility(View.GONE);
        }

        private void setImageCircleAsVisible(){
            imageCircle.setVisibility(View.VISIBLE);
            imageCircle.setBackgroundResource(R.drawable.image_circle);
            selectImageCircle.setText("");
        }

        private void setImageCircleAsUnvisible(){
            imageCircle.setVisibility(View.GONE);
        }

        private void checkAndShowOrHideSendImageButton(){
            boolean isListDoNotHaveCheckImage = true;
            for(ImageData imageData : imagesList){
                if(imageData.isCheck()){
                    isListDoNotHaveCheckImage = false;
                    break;
                }
            }
            if(isListDoNotHaveCheckImage){
                iShowableSendButton.hideButton();
            }else{
                iShowableSendButton.showButton();
            }
        }

    }
}
