package com.zwh.label.picker;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * author : zchu
 * date   : 2017/7/10
 * desc   :
 */

public class LabelSelectionFragment extends Fragment implements OnItemDragListener {
    private static final String BUNDLE_SELECTED_LABELS = "selected_labels";
    private static final String BUNDLE_ALWAY_SELECTED_LABELS = "alway_selected_labels";
    private static final String BUNDLE_UNSELECTED_LABELS = "unselected_labels";


    private RecyclerView mRecyclerView;
    private LabelSelectionAdapter mLabelSelectionAdapter;
    private ItemTouchHelper mHelper;
    private OnEditFinishListener mOnEditFinishListener;

    private boolean initMode = false;

    public static LabelSelectionFragment newInstance(ArrayList<Label> selectedLabels, ArrayList<Label> unselectedLabels) {

        Bundle args = new Bundle();
        args.putParcelableArrayList(BUNDLE_SELECTED_LABELS, selectedLabels);
        args.putParcelableArrayList(BUNDLE_UNSELECTED_LABELS, unselectedLabels);
        LabelSelectionFragment fragment = new LabelSelectionFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public static LabelSelectionFragment newInstance(ArrayList<Label> selectedLabels, ArrayList<Label> unselectedLabels, ArrayList<Label> alwaySelectedLabels) {

        Bundle args = new Bundle();
        args.putParcelableArrayList(BUNDLE_SELECTED_LABELS, selectedLabels);
        args.putParcelableArrayList(BUNDLE_ALWAY_SELECTED_LABELS, alwaySelectedLabels);
        args.putParcelableArrayList(BUNDLE_UNSELECTED_LABELS, unselectedLabels);
        LabelSelectionFragment fragment = new LabelSelectionFragment();
        fragment.setArguments(args);
        return fragment;
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.label_picker_fragment,container,false);
//        mRecyclerView = new RecyclerView(inflater.getContext());
//        mRecyclerView.setPadding(
//                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 15, getResources().getDisplayMetrics()),
//                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics()),
//                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 15, getResources().getDisplayMetrics()),
//                0
//        );
        mRecyclerView = view.findViewById(R.id.labelRecyclerView);
        mRecyclerView.setClipToPadding(false);
        mRecyclerView.setClipChildren(false);
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnEditFinishListener) {
            mOnEditFinishListener = (OnEditFinishListener) context;
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle arguments = getArguments();
        if (arguments != null) {
            final ArrayList<LabelSelectionItem> labelSelectionItems = new ArrayList<>();
            labelSelectionItems.add(new LabelSelectionItem(LabelSelectionItem.TYPE_LABEL_SELECTED_TITLE, "已选标签"));
            ArrayList<Label> alwaySelectedLabels = arguments.getParcelableArrayList(BUNDLE_ALWAY_SELECTED_LABELS);
            if (alwaySelectedLabels != null && alwaySelectedLabels.size() > 0) {
                for (Label alwaySelectedLabel : alwaySelectedLabels) {
                    labelSelectionItems.add(new LabelSelectionItem(LabelSelectionItem.TYPE_LABEL_ALWAY_SELECTED, alwaySelectedLabel));
                }
            }
            ArrayList<Label> selectedLabels = arguments.getParcelableArrayList(BUNDLE_SELECTED_LABELS);
            if (selectedLabels != null && selectedLabels.size() > 0) {
                for (Label selectedLabel : selectedLabels) {
                    labelSelectionItems.add(new LabelSelectionItem(LabelSelectionItem.TYPE_LABEL_SELECTED, selectedLabel));
                }
            }
            labelSelectionItems.add(new LabelSelectionItem(LabelSelectionItem.TYPE_LABEL_UNSELECTED_TITLE, "未选标签"));
            ArrayList<Label> unselectedLabels = arguments.getParcelableArrayList(BUNDLE_UNSELECTED_LABELS);
            if (unselectedLabels != null && unselectedLabels.size() > 0) {

                for (Label unselectedLabel : unselectedLabels) {
                    labelSelectionItems.add(new LabelSelectionItem(LabelSelectionItem.TYPE_LABEL_UNSELECTED, unselectedLabel));
                }
            }
            mLabelSelectionAdapter = new LabelSelectionAdapter(labelSelectionItems);
            mLabelSelectionAdapter.setInitMode(initMode);
            GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 4);
            gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    int itemViewType = mLabelSelectionAdapter.getItemViewType(position);
                    return itemViewType == LabelSelectionItem.TYPE_LABEL_SELECTED || itemViewType == LabelSelectionItem.TYPE_LABEL_UNSELECTED || itemViewType == LabelSelectionItem.TYPE_LABEL_ALWAY_SELECTED ? 1 : 4;
                }
            });
            mRecyclerView.setLayoutManager(gridLayoutManager);
            mRecyclerView.setAdapter(mLabelSelectionAdapter);

            ItemDragHelperCallBack callBack = new ItemDragHelperCallBack(this);
            mLabelSelectionAdapter.setOnChannelDragListener(this);
            mLabelSelectionAdapter.setOnEditFinishListener(mOnEditFinishListener);
            mHelper = new ItemTouchHelper(callBack);
            mHelper.attachToRecyclerView(mRecyclerView);
        }
    }

    /**
     * 设置初始编辑模式
     * @param isEditMode 是否为编辑模式 默认false
     */
    public void setInitMode(boolean isEditMode){
        this.initMode = isEditMode;
    }

    /**
     * 改变编辑模式
     */
    public void changeEditMode(){
        if(mLabelSelectionAdapter!=null){
            mLabelSelectionAdapter.setEditMode();
        }
    }

    @Override
    public void onItemMove(int starPos, int endPos) {
        List<LabelSelectionItem> data = mLabelSelectionAdapter.getData();
        LabelSelectionItem labelSelectionItem = data.get(starPos);
        //先删除之前的位置
        data.remove(starPos);
        //添加到现在的位置
        data.add(endPos, labelSelectionItem);
        mLabelSelectionAdapter.notifyItemMoved(starPos, endPos);
    }

    @Override
    public void onStarDrag(RecyclerView.ViewHolder viewHolder) {
        mHelper.startDrag(viewHolder);
    }

    public boolean cancelEdit() {
        return mLabelSelectionAdapter.cancelEdit();
    }



}
