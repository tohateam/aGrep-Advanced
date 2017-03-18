package ua.tohateam.aGrep;

import android.support.v7.widget.*;
import android.view.*;
import android.widget.*;
import ua.tohateam.aGrep.*;

public class MyHolder extends RecyclerView.ViewHolder 
implements View.OnClickListener
{
    //OUR VIEWS
    //ImageView img;
    TextView nameTxt,posTxt;
    ItemClickListener itemClickListener;

    public MyHolder(View itemView) {
        super(itemView);
        //this.img= (ImageView) itemView.findViewById(R.id.playerImage);
        this.nameTxt = (TextView) itemView.findViewById(R.id.file_name_row);
        this.posTxt = (TextView) itemView.findViewById(R.id.file_path_row);
        itemView.setOnClickListener(this);
    }
	
    @Override
    public void onClick(View v) {
        this.itemClickListener.onItemClick(v, getLayoutPosition());
    }
	
    public void setItemClickListener(ItemClickListener ic) {
        this.itemClickListener = ic;
    }
}
