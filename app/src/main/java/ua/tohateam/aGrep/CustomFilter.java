package ua.tohateam.aGrep;

import android.widget.*;
import java.util.*;
import ua.tohateam.aGrep.model.*;

public class CustomFilter extends Filter
{
    MyAdapter adapter;
    ArrayList<SearchModel> filterList;
	
    public CustomFilter(ArrayList<SearchModel> filterList, MyAdapter adapter) {
        this.adapter = adapter;
        this.filterList = filterList;
    }
	
    //FILTERING OCURS
    @Override
    protected FilterResults performFiltering(CharSequence constraint) {
        FilterResults results=new FilterResults();
        //CHECK CONSTRAINT VALIDITY
        if (constraint != null && constraint.length() > 0) {
            //CHANGE TO UPPER
            constraint = constraint.toString().toUpperCase();
            //STORE OUR FILTERED PLAYERS
            ArrayList<SearchModel> filteredPlayers=new ArrayList<>();
            for (int i=0;i < filterList.size();i++) {
                //CHECK
                if (filterList.get(i).getPath().toString().toUpperCase().contains(constraint)) {
                    //ADD PLAYER TO FILTERED PLAYERS
                    filteredPlayers.add(filterList.get(i));
                }
            }
            results.count = filteredPlayers.size();
            results.values = filteredPlayers;
        } else {
            results.count = filterList.size();
            results.values = filterList;
        }
        return results;
    }
    @Override
    protected void publishResults(CharSequence constraint, FilterResults results) {
		adapter.mSearchFilesModels = (ArrayList<SearchModel>) results.values;
        //REFRESH
        adapter.notifyDataSetChanged();
    }
}
