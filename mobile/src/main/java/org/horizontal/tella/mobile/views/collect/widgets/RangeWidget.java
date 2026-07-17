/*
 * Copyright (C) 2009 University of Washington
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.horizontal.tella.mobile.views.collect.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import org.horizontal.tella.mobile.R;
import org.javarosa.core.model.Constants;
import org.javarosa.core.model.RangeQuestion;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.IntegerData;
import org.javarosa.form.api.FormEntryPrompt;


/**
 * Based on ODK Collect SelectOneWidget.
 */
@SuppressLint("ViewConstructor")
public class RangeWidget extends QuestionWidget
        implements SeekBar.OnSeekBarChangeListener
{
    private SeekBar inputElement;

    private TextView rangeText;

    private int rangeStart;
    private int range;

    public RangeWidget(Context context, FormEntryPrompt prompt) {
        super(context, prompt);

        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        LayoutInflater inflater = LayoutInflater.from(getContext());

        inflater.inflate(R.layout.collect_widget_range, linearLayout, true);


        RangeQuestion q = (RangeQuestion) prompt.getQuestion();

        rangeStart = q.getRangeStart().intValue();
        final int rangeEnd = q.getRangeEnd().intValue();
        range = Math.abs(rangeEnd-rangeStart);

        final int rangeType = prompt.getDataType();
        if(rangeType == Constants.DATATYPE_INTEGER){
            inputElement = linearLayout.findViewById(R.id.seekBarDiscrete);
        }//todo: handle non integer types?

        inputElement.setId(QuestionWidget.newUniqueId());
        inputElement.setMax(range);

        rangeText = linearLayout.findViewById(R.id.rangeText);
        rangeText.setId(QuestionWidget.newUniqueId());

        if (formEntryPrompt.getAnswerValue() == null) {
            setAnswer(range/2);
        } else {
            rangeText.setText(formEntryPrompt.getAnswerText());
            Integer value = ((Integer)formEntryPrompt.getAnswerValue().getValue());
            inputElement.setProgress(Math.abs(q.getRangeStart().intValue())+value);
        }

        inputElement.setOnSeekBarChangeListener(this);

        addAnswerView(linearLayout);
    }

    protected void setAnswer(int value) {
        inputElement.setProgress(value);
        rangeText.setText(String.valueOf(value));
    }

    @Override
    public void clearAnswer() {
        setAnswer(range/2);
    }

    @Override
    public IAnswerData getAnswer() {
        return new IntegerData(inputElement.getProgress() + rangeStart);
    }

    @Override
    public void setFocus(Context context) {

    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        rangeText.setText(String.valueOf(seekBar.getProgress() - rangeStart));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}
}
