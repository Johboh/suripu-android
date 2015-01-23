package is.hello.sense.ui.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Insight;
import is.hello.sense.api.model.Question;
import is.hello.sense.graph.presenters.InsightsPresenter;
import is.hello.sense.graph.presenters.Presenter;
import is.hello.sense.graph.presenters.QuestionsPresenter;
import is.hello.sense.ui.adapter.InsightsAdapter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.InsightInfoDialogFragment;
import is.hello.sense.ui.dialogs.QuestionsDialogFragment;
import is.hello.sense.ui.widget.util.ListViews;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Markdown;

public class InsightsFragment extends InjectionFragment implements AdapterView.OnItemClickListener, SwipeRefreshLayout.OnRefreshListener {
    @Inject InsightsPresenter insightsPresenter;
    @Inject Markdown markdown;

    @Inject QuestionsPresenter questionsPresenter;

    private InsightsAdapter insightsAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;

    private ViewGroup questionContainer;
    private TextView questionAnswerTitle;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPresenter(insightsPresenter);
        addPresenter(questionsPresenter);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_insights, container, false);

        this.swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.fragment_insights_refresh_container);
        swipeRefreshLayout.setOnRefreshListener(this);
        Styles.applyRefreshLayoutStyle(swipeRefreshLayout);


        ListView listView = (ListView) view.findViewById(android.R.id.list);
        listView.setOnItemClickListener(this);
        Styles.addCardSpacingHeaderAndFooter(listView);


        this.questionContainer = (ViewGroup) inflater.inflate(R.layout.sub_fragment_new_question, listView, false);
        questionContainer.setVisibility(View.GONE);

        this.questionAnswerTitle = (TextView) questionContainer.findViewById(R.id.sub_fragment_new_question_title);

        Button skipQuestion = (Button) questionContainer.findViewById(R.id.sub_fragment_new_question_skip);
        Views.setSafeOnClickListener(skipQuestion, ignored -> questionsPresenter.skipQuestion());

        Button answerQuestion = (Button) questionContainer.findViewById(R.id.sub_fragment_new_question_answer);
        Views.setSafeOnClickListener(answerQuestion, ignored -> answerQuestion());

        FrameLayout layoutFix = new FrameLayout(getActivity());
        layoutFix.addView(questionContainer);
        ListViews.addHeaderView(listView, layoutFix, null, false);


        this.insightsAdapter = new InsightsAdapter(getActivity(), markdown, () -> swipeRefreshLayout.setRefreshing(false));
        listView.setAdapter(insightsAdapter);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindAndSubscribe(insightsPresenter.insights, insightsAdapter::bindInsights, insightsAdapter::insightsUnavailable);

        bindAndSubscribe(questionsPresenter.currentQuestion, currentQuestion -> {
            if (currentQuestion == null || isAnswerQuestionOpen()) {
                hideNewQuestion();
            } else {
                showNewQuestion(currentQuestion);
            }
        }, ignored -> questionContainer.setVisibility(View.GONE));
    }

    @Override
    public void onResume() {
        super.onResume();

        onRefresh();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);

        if (level >= Presenter.BASE_TRIM_LEVEL) {
            insightsAdapter.clear();
        }
    }


    //region Insights

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Insight insight = (Insight) adapterView.getItemAtPosition(position);
        InsightInfoDialogFragment dialogFragment = InsightInfoDialogFragment.newInstance(insight.getCategory());
        dialogFragment.show(getFragmentManager(), InsightInfoDialogFragment.TAG);
    }

    @Override
    public void onRefresh() {
        swipeRefreshLayout.setRefreshing(true);
        insightsPresenter.update();
        questionsPresenter.update();
    }

    //endregion


    //region Questions

    public void showNewQuestion(@NonNull Question question) {
        questionAnswerTitle.setText(question.getText());
        questionContainer.setVisibility(View.VISIBLE);
    }

    public void hideNewQuestion() {
        questionContainer.setVisibility(View.GONE);
    }

    public boolean isAnswerQuestionOpen() {
        return (getFragmentManager().findFragmentByTag(QuestionsDialogFragment.TAG) != null);
    }

    public void answerQuestion() {
        QuestionsDialogFragment dialogFragment = new QuestionsDialogFragment();
        dialogFragment.show(getActivity().getFragmentManager(), QuestionsDialogFragment.TAG);
        hideNewQuestion();
    }

    //endregion
}
