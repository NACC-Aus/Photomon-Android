package com.appiphany.nacc.screens;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.appiphany.nacc.R;
import com.appiphany.nacc.model.Photo;

/**
 * This class used to add note for photo
 */
public class AddNoteActivity extends BaseActivity {
    public static final String NOTE = "note";
    public static final String SHOW_NOTE = "show_note";
    public static final String ADD_NOTE = "add_note";

    private EditText mEtNote;

    private String mNoteString;
    private Photo mPhoto;

    // this variable used to check note is edit or add new
    private boolean mIsEditNote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_add_note);
        mEtNote = (EditText) findViewById(R.id.note_edittex);
        initActionBar();

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);

        Intent intent = getIntent();
        if (intent != null) {
            if (intent.getAction().equals(ADD_NOTE)) {
                mIsEditNote = false;
                mNoteString = intent.getStringExtra(NOTE);
            } else if (intent.getAction().equals(SHOW_NOTE)) {
                mIsEditNote = true;
                mPhoto = (Photo) intent.getSerializableExtra(BackgroundService.PHOTO_DATA_EXTRA);
                if (mPhoto != null) {
                    mNoteString = mPhoto.getNote();
                }
            }

            mEtNote.setText(mNoteString);
            mEtNote.setSelection(mEtNote.getText().length());
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void initActionBar() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayUseLogoEnabled(true);
        getSupportActionBar().setTitle(R.string.note);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add_note_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        } else if (item.getItemId() == R.id.done_menu) {
            hideKeyboard();
            if (mIsEditNote) {
                backToReviewScreen();
            } else {
                backToPreviewScreen();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    // back to ImagePreviewActivity
    private void backToPreviewScreen() {
        mNoteString = mEtNote.getText().toString();
        Intent intent = new Intent();
        intent.putExtra(NOTE, mNoteString);
        setResult(RESULT_OK, intent);
        finish();
    }

    // back to ImagePreviewActivity
    private void backToReviewScreen() {
        mNoteString = mEtNote.getText().toString();
        Intent intent = new Intent();
        mPhoto.setNote(mNoteString);
        intent.putExtra(BackgroundService.PHOTO_DATA_EXTRA, mPhoto);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mEtNote.getWindowToken(), 0);
    }

    @Override
    protected void onPause() {
        super.onPause();
        hideKeyboard();
    }

}
