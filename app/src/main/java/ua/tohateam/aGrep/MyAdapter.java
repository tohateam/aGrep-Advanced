package ua.tohateam.aGrep;

import android.content.*;
import android.support.design.widget.*;
import android.support.v7.widget.*;
import android.view.*;
import android.widget.*;
import java.util.*;
import ua.tohateam.aGrep.model.*;

public class MyAdapter extends RecyclerView.Adapter<MyHolder>
implements Filterable
{
    Context c;
    ArrayList<SearchModel> mSearchFilesModels, filterList;
    CustomFilter filter;

    public MyAdapter(Context ctx, ArrayList<SearchModel> SearchFilesModels) {
        this.c = ctx;
        this.mSearchFilesModels = SearchFilesModels;
        this.filterList = SearchFilesModels;
    }

    @Override
    public MyHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //CONVERT XML TO VIEW ONBJ
        View v= LayoutInflater.from(parent.getContext()).inflate(R.layout.files_item_row, null);
        //HOLDER
        MyHolder holder=new MyHolder(v);
        return holder;
    }

    //DATA BOUND TO VIEWS
    @Override
    public void onBindViewHolder(MyHolder holder, int position) {
        //BIND DATA
        holder.posTxt.setText(mSearchFilesModels.get(position).getPath().toString());
        holder.nameTxt.setText(mSearchFilesModels.get(position).getName());
//        holder.img.setImageResource(SearchFilesModels.get(position).getImg());
        //IMPLEMENT CLICK LISTENET
        holder.setItemClickListener(new ItemClickListener() {
				@Override
				public void onItemClick(View v, int pos) {
					Snackbar.make(v, mSearchFilesModels.get(pos).getName(), Snackbar.LENGTH_SHORT).show();
				}
			});
    }
	
    //GET TOTAL NUM OF SearchFilesModelS
    @Override
    public int getItemCount() {
        return mSearchFilesModels.size();
    }
	
    //RETURN FILTER OBJ
    @Override
    public Filter getFilter() {
        if (filter == null) {
            filter = new CustomFilter(filterList, this);
        }
        return filter;
    }
}
