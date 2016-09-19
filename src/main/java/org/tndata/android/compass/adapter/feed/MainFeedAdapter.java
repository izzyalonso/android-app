package org.tndata.android.compass.adapter.feed;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;
import android.widget.Space;

import org.tndata.android.compass.CompassApplication;
import org.tndata.android.compass.R;
import org.tndata.android.compass.databinding.CardBaseItemBinding;
import org.tndata.android.compass.holder.BaseItemCardHolder;
import org.tndata.android.compass.model.FeedData;
import org.tndata.android.compass.model.Goal;
import org.tndata.android.compass.model.Reward;
import org.tndata.android.compass.model.TDCGoal;
import org.tndata.android.compass.model.UpcomingAction;
import org.tndata.android.compass.parser.Parser;
import org.tndata.android.compass.parser.ParserModels;
import org.tndata.android.compass.util.API;
import org.tndata.android.compass.util.CompassUtil;

import java.util.List;

import es.sandwatch.httprequests.HttpRequest;
import es.sandwatch.httprequests.HttpRequestError;


/**
 * Adapter for the main feed.
 *
 * @author Ismael Alonso
 * @version 2.1.0
 */
public class MainFeedAdapter
        extends RecyclerView.Adapter
        implements
                HttpRequest.RequestCallback,
                Parser.ParserCallback{

    private static final String TAG = "MainFeedAdapter";

    //Item view types
    private static final int TYPE_BLANK = 0;
    private static final int TYPE_WELCOME = TYPE_BLANK+1;
    public static final int TYPE_UP_NEXT = TYPE_WELCOME+1;
    private static final int TYPE_SUGGESTION = TYPE_UP_NEXT+1;
    public static final int TYPE_STREAKS = TYPE_SUGGESTION+1;
    private static final int TYPE_REWARD = TYPE_STREAKS+1;
    private static final int TYPE_MY_GOALS = TYPE_REWARD +1;
    private static final int TYPE_GOAL_SUGGESTIONS = TYPE_MY_GOALS+1;
    private static final int TYPE_OTHER = TYPE_MY_GOALS+1;


    final Context mContext;
    final Listener mListener;

    private FeedData mFeedData;
    private FeedUtil mFeedUtil;
    private TDCGoal mSuggestion;

    private GoalsHolder<Goal> mMyGoalsHolder;
    private GoalsHolder<TDCGoal> mSuggestionsHolder;

    private int mGetMoreGoalsRC;


    /**
     * Constructor.
     *
     * @param context the context,
     * @param listener the listener.
     * @param initialSuggestion true the feed should display an initial suggestion.
     */
    public MainFeedAdapter(@NonNull Context context, @NonNull Listener listener,
                           boolean initialSuggestion){
        mContext = context;
        mListener = listener;
        mFeedData = ((CompassApplication)mContext.getApplicationContext()).getFeedData();

        if (mFeedData == null){
            mListener.onNullData();
        }
        else{
            CardTypes.setDataSource(mFeedData);
            List<TDCGoal> suggestions = mFeedData.getSuggestions();
            if (suggestions.isEmpty()){
                mSuggestion = null;
            }
            else{
                mSuggestion = suggestions.get((int)(Math.random()*suggestions.size()));
            }
            mFeedUtil = new FeedUtil(this);
        }

        if (mSuggestion != null){
            if (initialSuggestion){
                CardTypes.displaySuggestion(true);
            }
            else{
                new Handler().postDelayed(new Runnable(){
                    @Override
                    public void run(){
                        CardTypes.displaySuggestion(true);
                        notifyItemInserted(CardTypes.getSuggestionPosition());
                        notifyItemRangeChanged(CardTypes.getSuggestionPosition() + 1, getItemCount() - 1);
                    }
                }, 2000);
            }
        }
    }


    /*------------------------------------*
     * OVERRIDDEN ADAPTER RELATED METHODS *
     *------------------------------------*/

    @Override
    public int getItemViewType(int position){
        if (position == 0){
            return TYPE_BLANK;
        }
        if (CardTypes.isWelcome(position)){
            return TYPE_WELCOME;
        }
        if (CardTypes.isUpNext(position)){
            return TYPE_UP_NEXT;
        }
        if (CardTypes.isStreaks(position)){
            return TYPE_STREAKS;
        }
        if (CardTypes.isSuggestion(position)){
            return TYPE_SUGGESTION;
        }
        if (CardTypes.isReward(position)){
            return TYPE_REWARD;
        }
        if (CardTypes.isMyGoals(position)){
            return TYPE_MY_GOALS;
        }
        if (CardTypes.isGoalSuggestions(position)){
            return TYPE_GOAL_SUGGESTIONS;
        }
        return TYPE_OTHER;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, final int viewType){
        RecyclerView.ViewHolder holder = null;
        //Log.d(TAG, "onCreateViewHolder(): " + viewType);
        if (viewType == TYPE_BLANK){
            holder = new RecyclerView.ViewHolder(new Space(mContext)){};
        }
        else if (viewType == TYPE_WELCOME){
            LayoutInflater inflater = LayoutInflater.from(mContext);
            View view = inflater.inflate(R.layout.card_welcome, parent, false);
            view.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    mListener.onInstructionsSelected();
                }
            });
            holder = new RecyclerView.ViewHolder(view){};
        }
        else if (viewType == TYPE_UP_NEXT || viewType == TYPE_REWARD){
            LayoutInflater inflater = LayoutInflater.from(mContext);
            CardBaseItemBinding binding = DataBindingUtil.inflate(
                    inflater, R.layout.card_base_item, parent, false
            );
            holder = new BaseItemCardHolder(binding);
        }
        else if (viewType == TYPE_STREAKS){
            LayoutInflater inflater = LayoutInflater.from(mContext);
            holder = new StreaksHolder(this, inflater.inflate(R.layout.card_streaks, parent, false));
        }
        else if (viewType == TYPE_SUGGESTION){
            LayoutInflater inflater = LayoutInflater.from(mContext);
            holder = new GoalSuggestionHolder(this, inflater.inflate(R.layout.card_goal_suggestion, parent, false));
        }
        else if (viewType == TYPE_MY_GOALS){
            if (mMyGoalsHolder == null){
                LayoutInflater inflater = LayoutInflater.from(mContext);
                View rootView = inflater.inflate(R.layout.card_goals, parent, false);
                mMyGoalsHolder = new GoalsHolder<>(this, rootView);
            }
            holder = mMyGoalsHolder;
        }
        else if (viewType == TYPE_GOAL_SUGGESTIONS){
            if (mSuggestionsHolder == null){
                LayoutInflater inflater = LayoutInflater.from(mContext);
                View rootView = inflater.inflate(R.layout.card_goals, parent, false);
                mSuggestionsHolder = new GoalsHolder<>(this, rootView);
            }
            holder = mSuggestionsHolder;
        }

        final RecyclerView.ViewHolder vtHolder = holder;
        ViewTreeObserver vto = vtHolder.itemView.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener(){
            @Override
            public void onGlobalLayout(){
                if (Build.VERSION.SDK_INT < 16){
                    vtHolder.itemView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
                else{
                    vtHolder.itemView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
                mListener.onItemLoaded(vtHolder.itemView, viewType);
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder rawHolder, int position){
        //Log.d(TAG, "onBindViewHolder(): " + position);
        //This is a possible fix to a crash where the application gets destroyed and the
        //  user data gets invalidated. In such a case, the app should restart and fetch
        //  the user data again. Bottomline, do not keep going
        if (mFeedData == null){
            return;
        }

        //Blank space
        if (position == 0){
            int width = CompassUtil.getScreenWidth(mContext);
            LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, (int)((width*2/3)*0.8));
            rawHolder.itemView.setLayoutParams(params);
            rawHolder.itemView.setVisibility(View.INVISIBLE);
        }
        //Up next
        else if (CardTypes.isUpNext(position)){
            BaseItemCardHolder holder = (BaseItemCardHolder)rawHolder;
            holder.setIcon(R.drawable.ic_up_next);

            UpcomingAction action = mFeedData.getUpNextAction();
            if (action == null){
                if (mFeedData.getProgress().getTotalActions() != 0){
                    holder.setTitle(R.string.card_up_next_title_completed);
                    holder.setSubtitle(R.string.card_up_next_subtitle_completed);
                }
                else{
                    holder.setTitle(R.string.card_up_next_title_empty);
                    holder.setSubtitle(R.string.card_up_next_subtitle_empty);
                }
            }
            else{
                holder.setTitle(action.getTitle());
            }
        }
        //Streaks
        else if (CardTypes.isStreaks(position)){
            ((StreaksHolder)rawHolder).bind(mFeedData.getStreaks());
        }
        //Goal suggestion card
        else if (CardTypes.isSuggestion(position)){
            GoalSuggestionHolder holder = (GoalSuggestionHolder)rawHolder;
            holder.mTitle.setText(mSuggestion.getTitle());
        }
        //Reward
        else if (CardTypes.isReward(position)){
            BaseItemCardHolder holder = (BaseItemCardHolder)rawHolder;
            Reward reward = mFeedData.getReward();

            holder.setIcon(reward.getIcon());
            holder.setTitle(reward.getHeader());
            holder.setSubtitle(reward.getMessage().substring(0, 30) + "...");
        }
        //My goals
        else if (CardTypes.isMyGoals(position)){
            if (mMyGoalsHolder.getItemCount() == 0){
                mMyGoalsHolder.bind(mContext.getString(R.string.card_my_goals_header));
                mMyGoalsHolder.setGoals(mFeedData.getGoals());
                if (mFeedData.getNextGoalBatchUrl() == null){
                    mMyGoalsHolder.hideFooter();
                }
            }
        }
        else if (CardTypes.isGoalSuggestions(position)){
            if (mSuggestionsHolder.getItemCount() == 0){
                mSuggestionsHolder.bind(mContext.getString(R.string.card_suggestions_header));
                mSuggestionsHolder.setGoals(mFeedData.getSuggestions());
                mSuggestionsHolder.hideFooter();
            }
        }
    }

    @Override
    public int getItemCount(){
        return CardTypes.getItemCount();
    }


    /*----------------------*
     * FEED ADAPTER METHODS *
     *----------------------*/

    /**
     * Gets the position of the goals card.
     *
     * @return the index of the goals card.
     */
    public int getGoalsPosition(){
        return CardTypes.getGoalsPosition();
    }

    /**
     * Updates the data set (up next, upcoming, and my goals).
     */
    public void updateDataSet(){
        updateUpcoming();
        updateMyGoals();
    }

    /**
     * Updates upcoming and up next.
     */
    public void updateUpcoming(){

    }

    /**
     * Updates my goals.
     */
    public void updateMyGoals(){
        if (mMyGoalsHolder != null){
            mMyGoalsHolder.updateGoals();
            notifyItemChanged(CardTypes.getGoalsPosition());
        }
    }


    /*------------------------*
     * ACTION RELATED METHODS *
     *------------------------*/

    /**
     * Marks an action as done in he data set.
     *
     * @param action the action to be marked as done.
     */
    public void didIt(UpcomingAction action){
        mFeedData.removeUpcomingActionX(action, true);
    }


    /*----------------------*
     * GOAL RELATED METHODS *
     *----------------------*/

    public void notifyGoalRemoved(int position){
        if (mMyGoalsHolder != null){
            mMyGoalsHolder.notifyGoalRemoved(position);
        }
    }

    /**
     * Shows the suggestion card popup menu.
     *
     * @param anchor the anchor view.
     */
    void showSuggestionPopup(View anchor){
        mFeedUtil.showSuggestionPopup(anchor);
    }

    /**
     * Refreshes the suggestion card.
     */
    void refreshSuggestion(){
        List<TDCGoal> suggestions = mFeedData.getSuggestions();
        mSuggestion = suggestions.get((int)(Math.random()*suggestions.size()));
        notifyItemChanged(CardTypes.getSuggestionPosition());
    }

    /**
     * Dismisses the suggestion card.
     */
    public void dismissSuggestion(){
        CardTypes.displaySuggestion(false);
        notifyItemRemoved(CardTypes.getSuggestionPosition());
        notifyItemRangeChanged(CardTypes.getSuggestionPosition() + 1, getItemCount() - 1);
        mListener.onSuggestionDismissed();
    }

    /**
     * Opens up a view to display the suggestion in the suggestion card.
     */
    void viewSuggestion(){
        mListener.onSuggestionSelected(mSuggestion);
    }


    /*------------------------*
     * FOOTER RELATED METHODS *
     *------------------------*/

    /**
     * Loads the next batch of actions into the feed.
     */
    void moreActions(){

    }

    /**
     * Loads the next batch of goals into the feed.
     */
    void moreGoals(){
        mGetMoreGoalsRC = HttpRequest.get(this, mFeedData.getNextGoalBatchUrl());
    }


    /*-------------------------*
     * REQUEST RELATED METHODS *
     *-------------------------*/

    @Override
    public void onRequestComplete(int requestCode, String result){
        if (requestCode == mGetMoreGoalsRC){
            if (mFeedData.getNextGoalBatchUrl().contains("customgoals")){
                Parser.parse(result, ParserModels.CustomGoalsResultSet.class, this);
            }
            else{
                Parser.parse(result, ParserModels.UserGoalsResultSet.class, this);
            }
        }
    }

    @Override
    public void onRequestFailed(int requestCode, HttpRequestError error){

    }

    @Override
    public void onProcessResult(int requestCode, ParserModels.ResultSet result){

    }

    @Override
    public void onParseSuccess(int requestCode, ParserModels.ResultSet result){
        if (result instanceof ParserModels.CustomGoalsResultSet){
            ParserModels.CustomGoalsResultSet set = (ParserModels.CustomGoalsResultSet)result;
            String url = set.next;
            if (url == null){
                url = API.URL.getUserGoals();
            }
            if (API.STAGING && url.startsWith("https")){
                url = url.replaceFirst("s", "");
            }
            mMyGoalsHolder.prepareGoalAddition();
            mFeedData.addGoals(set.results, url);
            mMyGoalsHolder.onGoalsAdded();
        }
        else if (result instanceof ParserModels.UserGoalsResultSet){
            ParserModels.UserGoalsResultSet set = (ParserModels.UserGoalsResultSet)result;
            mMyGoalsHolder.prepareGoalAddition();
            String url = set.next;
            if (url == null){
                mMyGoalsHolder.hideFooter();
            }
            else{
                if (API.STAGING && url.startsWith("https")){
                    url = url.replaceFirst("s", "");
                }
            }
            mFeedData.addGoals(set.results, url);
            mMyGoalsHolder.onGoalsAdded();
        }
    }

    @Override
    public void onParseFailed(int requestCode){

    }

    /**
     * Parent class of all the view holders in for the main feed adapter. Provides a reference
     * to the adapter that needs to be passed through the constructor.
     *
     * @author Ismael Alonso
     * @version 1.0.0
     */
    abstract static class ViewHolder extends RecyclerView.ViewHolder{
        protected MainFeedAdapter mAdapter;

        /**
         * Constructor,
         *
         * @param adapter a reference to the adapter that will handle the holder.
         * @param rootView the root view of this adapter.
         */
        protected ViewHolder(MainFeedAdapter adapter, View rootView){
            super(rootView);
            mAdapter = adapter;
        }
    }


    /**
     * Listener interface for the main feed adapter.
     *
     * @author Ismael Alonso
     * @version 1.0.0
     */
    public interface Listener{
        /**
         * Called when items are created.
         *
         * @param view the root view of the item that was loaded.
         * @param type the type of the item that was loaded.
         */
        void onItemLoaded(View view, int type);

        /**
         * Called when the user data is null.
         */
        void onNullData();

        /**
         * Called when the welcome card is tapped.
         */
        void onInstructionsSelected();

        /**
         * Called when a suggestion is dismissed.
         */
        void onSuggestionDismissed();

        /**
         * Called when a goal from a goal list is selected.
         *
         * @param suggestion the selected goal suggestion.
         */
        void onSuggestionSelected(TDCGoal suggestion);

        /**
         * Called when a goal is selected.
         *
         * @param goal the selected goal.
         */
        void onGoalSelected(Goal goal);

        /**
         * Called when the streaks card is tapped.
         *
         * @param streaks list of Feedback streaks data.
         */
        void onStreaksSelected(List<FeedData.Streak> streaks);

        /**
         * Called when an action card is tapped.
         *
         * @param action the action being displayed at the card.
         */
        void onActionSelected(UpcomingAction action);
    }
}
