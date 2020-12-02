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

package com.hike.messagingapp.Model;

import android.app.Application;
import android.util.LruCache;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.common.model.RemoteModelManager;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.TranslateRemoteModel;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class TranslateViewModel extends AndroidViewModel {
    // This specifies the number of translators instance we want to keep in our LRU cache.
    // Each instance of the translator is built with different options based on the source
    // language and the target language, and since we want to be able to manage the number of
    // translator instances to keep around, an LRU cache is an easy way to achieve this.
    private static final int NUM_TRANSLATORS = 3;

    private final RemoteModelManager modelManager;
    private final LruCache<TranslatorOptions, Translator> translators =
            new LruCache<TranslatorOptions, Translator>(NUM_TRANSLATORS) {
                @Override
                public Translator create(TranslatorOptions options) {
                    return Translation.getClient(options);
                }

                @Override
                public void entryRemoved(boolean evicted, TranslatorOptions key,
                                         Translator oldValue, Translator newValue) {
                    oldValue.close();
                }
            };



    public TranslatorOptions options = new TranslatorOptions.Builder()
            .setSourceLanguage("en")
            .setTargetLanguage("es")
            .build();
    public Observer<Language> languageObserver;




    public MutableLiveData<Language> sourceLang = new MutableLiveData<>();
    public MutableLiveData<Language> targetLang = new MutableLiveData<>();
    public MutableLiveData<String> sourceText = new MutableLiveData<>();
    public MediatorLiveData<ResultOrError> translatedText = new MediatorLiveData<>();
    public MutableLiveData<List<String>> availableModels =
            new MutableLiveData<>();
    public String srcTxt = "";

    public TranslateViewModel(@NonNull Application application) {
        super(application);
        modelManager = RemoteModelManager.getInstance();

        // Create a translation result or error object.
        final OnCompleteListener<String> processTranslation = new OnCompleteListener<String>() {
            @Override
            public void onComplete(@NonNull Task<String> task) {
                if (task.isSuccessful()) {
                    translatedText.setValue(new ResultOrError(task.getResult(), null));
                } else {
                    translatedText.setValue(new ResultOrError(null, task.getException()));
                }
                // Update the list of downloaded models as more may have been
                // automatically downloaded due to requested translation.
                fetchDownloadedModels();
            }
        };

        // Start translation if any of the following change: input text, source lang, target lang.
        translatedText.addSource(sourceText, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                translate().addOnCompleteListener(processTranslation);
            }
        });
        languageObserver = new Observer<Language>() {
            @Override
            public void onChanged(@Nullable Language language) {
                translate().addOnCompleteListener(processTranslation);
            }
        };
        translatedText.addSource(sourceLang, languageObserver);
        translatedText.addSource(targetLang, languageObserver);

        // Update the list of downloaded models.
        fetchDownloadedModels();
    }

    // Gets a list of all available translation languages.
    public List<Language> getAvailableLanguages() {
        List<Language> languages = new ArrayList<>();
        List<String> languageIds = TranslateLanguage.getAllLanguages();
        for (String languageId : languageIds) {
            languages.add(
                    new Language(TranslateLanguage.fromLanguageTag(languageId)));
        }
        return languages;
    }

    private TranslateRemoteModel getModel(String languageCode) {
        return new TranslateRemoteModel.Builder(languageCode).build();
    }

    // Updates the list of downloaded models available for local translation.
    private void fetchDownloadedModels() {
        modelManager.getDownloadedModels(TranslateRemoteModel.class).addOnSuccessListener(
                new OnSuccessListener<Set<TranslateRemoteModel>>() {
                    @Override
                    public void onSuccess(Set<TranslateRemoteModel> remoteModels) {
                        List<String> modelCodes = new ArrayList<>(remoteModels.size());
                        for (TranslateRemoteModel model : remoteModels) {
                            modelCodes.add(model.getLanguage());
                        }
                        Collections.sort(modelCodes);
                        availableModels.setValue(modelCodes);
                    }
                });
    }

    // Starts downloading a remote model for local translation.
    public void downloadLanguage(Language language) {
        TranslateRemoteModel model =
                getModel(TranslateLanguage.fromLanguageTag(language.getCode()));
        modelManager.download(model, new DownloadConditions.Builder().build())
                .addOnCompleteListener(
                        new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                fetchDownloadedModels();
                            }
                        });
    }

    // Deletes a locally stored translation model.
    public void deleteLanguage(Language language) {
        TranslateRemoteModel model =
                getModel(TranslateLanguage.fromLanguageTag(language.getCode()));
        modelManager.deleteDownloadedModel(model).addOnCompleteListener(
                new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        fetchDownloadedModels();
                    }
                });
    }

    public Task<String> translate() {
        final String text = sourceText.getValue();
        final Language source = sourceLang.getValue();
        final Language target = targetLang.getValue();
        if (source == null || target == null || text == null || text.isEmpty()) {
            return Tasks.forResult("");
        }
        String sourceLangCode =
                TranslateLanguage.fromLanguageTag(source.getCode());
        String targetLangCode =
                TranslateLanguage.fromLanguageTag(target.getCode());
        final TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(sourceLangCode)
                .setTargetLanguage(targetLangCode)
                .build();
        return translators.get(options).downloadModelIfNeeded().continueWithTask(
                new Continuation<Void, Task<String>>() {
                    @Override
                    public Task<String> then(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            return translators.get(options).translate(text);
                        } else {
                            Exception e = task.getException();
                            if (e == null) {
                                e = new Exception(task.getException());
                            }
                            return Tasks.forException(e);
                        }
                    }
                });
    }

    public void setSrcText(String message) {
    }

    /**
     * Holds the result of the translation or any error.
     */
    public static class ResultOrError {
        final @Nullable
        public String result;
        final @Nullable
        public Exception error;

        ResultOrError(@Nullable String result, @Nullable Exception error) {
            this.result = result;
            this.error = error;
        }
    }

    /**
     * Holds the language code (i.e. "en") and the corresponding localized full language name
     * (i.e. "English")
     */
    public static class Language implements Comparable<Language> {
        private String code;

        public Language(String code) {
            this.code = code;
        }

        String getDisplayName() {
            return new Locale(code).getDisplayName();
        }

        public String getCode() {
            return code;
        }

        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }

            if (!(o instanceof Language)) {
                return false;
            }

            Language otherLang = (Language) o;
            return otherLang.code.equals(code);
        }

        @NonNull
        public String toString() {
            return code + " - " + getDisplayName();
        }

        @Override
        public int hashCode() {
            return code.hashCode();
        }

        @Override
        public int compareTo(@NonNull Language o) {
            return this.getDisplayName().compareTo(o.getDisplayName());
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Each new instance of a translator needs to be closed appropriately. Here we utilize the
        // ViewModel's onCleared() to clear our LruCache and close each Translator instance when
        // this ViewModel is no longer used and destroyed.
        translators.evictAll();
    }


    public void setSrcTxt(String s) {
        srcTxt = s;
    }

    public Translator myTranslator() {
        //Log.e("WWWWWWWWWWWWWWW", srcTxt );



        return translators.get(options);

    }


    public void setLanguages(String source, String target) {

        options = new TranslatorOptions.Builder()
                .setSourceLanguage(source)
                .setTargetLanguage(target)
                .build();

        //translatedText.addSource(source, languageObserver);
        //translatedText.addSource(targetLang, languageObserver);


    }


    public Translator getTranslator(){
        return translators.get(options);
    }
}
/*

    public void setSourceLang(String source){

        SharedPreferences prefs = getApplication().getSharedPreferences("languages", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString("source", source);
        editor.apply();

        sourceLang.setValue(new TranslateViewModel.Language(source));

        //translatedText.addSource(sourceLang, languageObserver);

    }

    public void setTargetLang(String target){
        SharedPreferences prefs = getApplication().getSharedPreferences("languages", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString("source", target);
        editor.apply();

        targetLang.setValue(new TranslateViewModel.Language(target));

        //translatedText.addSource(targetLang, languageObserver);

    }


    void readLanguagePref(){
        SharedPreferences prefs = getApplication().getSharedPreferences("languages", Context.MODE_PRIVATE);
        String source = prefs.getString("source", "error");
        String target = prefs.getString("target", "error");
        if(source!=null) {
            //sourceLang.setValue(new TranslateViewModel.Language(source));
            translatedText.addSource(sourceLang, languageObserver);
        } else{
            //sourceLang.setValue(new TranslateViewModel.Language("es"));
            translatedText.addSource(sourceLang, languageObserver);
        }
        if(target!=null) {
            //targetLang.setValue(new TranslateViewModel.Language(target));
            translatedText.addSource(targetLang, languageObserver);
        } else{
            //targetLang.setValue(new TranslateViewModel.Language("en"));
            translatedText.addSource(targetLang, languageObserver);
        }
    }

*/

