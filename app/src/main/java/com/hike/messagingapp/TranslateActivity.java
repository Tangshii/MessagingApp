/*
 * Copyright 2019 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.hike.messagingapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.hike.messagingapp.Model.TranslateViewModel;
import com.rengwuxian.materialedittext.MaterialEditText;

import java.util.List;

public class TranslateActivity extends AppCompatActivity {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_translate);

        // put intent extra back to messages
        Intent intent = getIntent();
        final String receiverId = intent.getStringExtra("receiverId");
        final boolean main = intent.getBooleanExtra("main", false);

        // set up the toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Translation");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() { // set back button
            @Override
            public void onClick(View view) {
                Toast.makeText(TranslateActivity.this, "Translation settings saved", Toast.LENGTH_SHORT).show();
                if(main)
                    startActivity(new Intent(TranslateActivity.this, MainActivity.class));
                else{
                    Intent intent = new Intent(TranslateActivity.this, MessageActivity.class);
                    intent.putExtra("userid", receiverId); //put the receiver uid
                    TranslateActivity.this.startActivity(intent);
                }
            }
        });

        // find views
        final Button send_msg = findViewById(R.id.send_msg);
        if(main)
            send_msg.setVisibility(View.GONE);
        final Button switchButton = findViewById(R.id.buttonSwitchLang);
        final ToggleButton sourceSyncButton = findViewById(R.id.buttonSyncSource);
        final ToggleButton targetSyncButton = findViewById(R.id.buttonSyncTarget);
        final TextInputEditText srcTextView = findViewById(R.id.sourceText);
        final TextView targetTextView = findViewById(R.id.targetText);
        final TextView downloadedModelsTextView = findViewById(R.id.downloadedModels);
        final Spinner sourceLangSelector = findViewById(R.id.sourceLangSelector);
        final Spinner targetLangSelector = findViewById(R.id.targetLangSelector);
        ConstraintLayout layout = findViewById(R.id.translate);

        // get color shared preferences and set view to the colors
        SharedPreferences prefs = getApplication().getSharedPreferences("colorPref", Context.MODE_PRIVATE);
        int primary = prefs.getInt("primary", -1);
        int secondary = prefs.getInt("secondary", -1);
        if(primary != -1){
            sourceSyncButton.setBackgroundColor(primary);
            targetSyncButton.setBackgroundColor(primary);
            send_msg.setBackgroundColor(primary);
            toolbar.setBackgroundColor(primary);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Window window = getWindow();
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.setStatusBarColor(primary);
            }
        }
        if(secondary != -1){
            layout.setBackgroundColor(secondary);
        }

        // translation model
        final TranslateViewModel viewModel = ViewModelProviders.of(this).get(TranslateViewModel.class);

        // Get available language list and set up source and target language spinners
        // with default selections.
        final ArrayAdapter<TranslateViewModel.Language> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, viewModel.getAvailableLanguages());
        sourceLangSelector.setAdapter(adapter);
        targetLangSelector.setAdapter(adapter);
        sourceLangSelector.setSelection(adapter.getPosition(new TranslateViewModel.Language("en")));
        targetLangSelector.setSelection(adapter.getPosition(new TranslateViewModel.Language("es")));

        // set shared preferences for languages if any
        prefs = getApplication().getSharedPreferences("languages", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        String source = prefs.getString("source", "error");
        String target = prefs.getString("target", "error");

        if(source=="error") {
            sourceLangSelector.setSelection(adapter.getPosition(new TranslateViewModel.Language("en")));
            editor.putString("source", source).apply();

        } else{
            sourceLangSelector.setSelection(adapter.getPosition(new TranslateViewModel.Language(source)));
            editor.putString("source", source).apply();
        }

        if(target=="error") {
            targetLangSelector.setSelection(adapter.getPosition(new TranslateViewModel.Language("es")));
            editor.putString("target", target).apply();
        } else{
            targetLangSelector.setSelection(adapter.getPosition(new TranslateViewModel.Language(target)));
            editor.putString("target", target).apply();
        }

        // drop down menu for source languages
        sourceLangSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setProgressText(targetTextView);
                viewModel.sourceLang.setValue(adapter.getItem(position));

                setSourceLang(adapter.getItem(position).getCode());
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                targetTextView.setText("");
            }
        });

        // drop down menu for target languages
        targetLangSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setProgressText(targetTextView);
                viewModel.targetLang.setValue(adapter.getItem(position));

                setTargetLang(adapter.getItem(position).getCode());
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                targetTextView.setText("");
            }
        });

        // switches source and target language
        switchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setProgressText(targetTextView);
                int sourceLangPosition = sourceLangSelector.getSelectedItemPosition();
                sourceLangSelector.setSelection(targetLangSelector.getSelectedItemPosition());
                targetLangSelector.setSelection(sourceLangPosition);

                setSourceLang(targetLangSelector.getSelectedItem().toString());
                setTargetLang(sourceLangSelector.getSelectedItem().toString());
            }
        });

        // Set up toggle buttons to delete or download remote models locally.
        sourceSyncButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                TranslateViewModel.Language language = adapter.getItem(sourceLangSelector.getSelectedItemPosition());
                if (isChecked) {
                    viewModel.downloadLanguage(language);
                } else {
                    viewModel.deleteLanguage(language);
                }
            }
        });
        targetSyncButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                TranslateViewModel.Language language = adapter.getItem(targetLangSelector.getSelectedItemPosition());
                if (isChecked) {
                    viewModel.downloadLanguage(language);
                } else {
                    viewModel.deleteLanguage(language);
                }
            }
        });

        // Translate input text as it is typed
        srcTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                setProgressText(targetTextView);
                viewModel.sourceText.postValue(s.toString());
            }
        });
        viewModel.translatedText.observe(this, new Observer<TranslateViewModel.ResultOrError>() {
            @Override
            public void onChanged(TranslateViewModel.ResultOrError resultOrError) {
                if (resultOrError.error != null) {
                    srcTextView.setError(resultOrError.error.getLocalizedMessage());
                } else {
                    targetTextView.setText(resultOrError.result);
                }
            }
        });

        // Update sync toggle button states based on downloaded models list.
        viewModel.availableModels.observe(this, new Observer<List<String>>() {
            @Override
            public void onChanged(@Nullable List<String> translateRemoteModels) {
                String output = TranslateActivity.this.getString(R.string.downloaded_models_label,
                        translateRemoteModels);
                downloadedModelsTextView.setText(output);
                sourceSyncButton.setChecked(translateRemoteModels.contains(
                        adapter.getItem(sourceLangSelector.getSelectedItemPosition()).getCode()));
                targetSyncButton.setChecked(translateRemoteModels.contains(
                        adapter.getItem(targetLangSelector.getSelectedItemPosition()).getCode()));
            }
        });

        send_msg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(targetTextView.getText().toString()!=null) {
                    Intent intent = new Intent(TranslateActivity.this, MessageActivity.class);
                    intent.putExtra("translated_msg", targetTextView.getText().toString());
                    intent.putExtra("userid", receiverId); //put the receiver uid
                    TranslateActivity.this.startActivity(intent);
                }

            }
        });
    }

    private void setProgressText(TextView tv) {
        tv.setText(TranslateActivity.this.getString(R.string.translate_progress));
    }

    public void setSourceLang(String source){
        SharedPreferences prefs = getApplication().getSharedPreferences("languages", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString("source", source);
        editor.apply();
    }

    public void setTargetLang(String target){
        SharedPreferences prefs = getApplication().getSharedPreferences("languages", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString("target", target);
        editor.apply();
    }

    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if ( v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }
}
