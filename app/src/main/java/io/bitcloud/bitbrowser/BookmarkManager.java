package io.bitcloud.bitbrowser;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

class BookmarkManager {

    private SQLiteDatabase database;
    private Adapter adapter;
    private ArrayList<BookmarksObject> arrayList;

    private final static String DB_TABLE = "BookmarkManager";
    private final static String ROW_ID = "_id";
    private final static String ROW_URL = "url";
    private final static String ROW_TITLE = "title";
    private final static String ROW_FAVICON = "favicon";
    private final static String ROW_TIMESTAMP = "timestamp";

    BookmarkManager(Activity activity){
        BookmarksHelper dbHelper = new BookmarksHelper(activity.getBaseContext());
        database = dbHelper.getWritableDatabase();

        this.arrayList = new ArrayList<>();

        this.adapter = new Adapter(activity, 0, arrayList);

        populateAdapter();
    }

    long add(String url, String title, Bitmap bitmap) {
        byte[] favicon = new byte[0];
        if(bitmap != null) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 0, stream);
            favicon = stream.toByteArray();
        }

        ContentValues cv = new  ContentValues();
        cv.put(ROW_URL, url);
        cv.put(ROW_TITLE, title);
        cv.put(ROW_FAVICON, favicon);
        cv.put(ROW_TIMESTAMP, System.currentTimeMillis());
        long result = database.insert(DB_TABLE, null, cv);

        this.populateAdapter();

        return result;
    }

    int remove(String url) {
        int count = database.delete(DB_TABLE, ROW_URL + " = ?", new String[] { url });
        populateAdapter();

        return count;
    }

    boolean contains(String url) {
        String[] cols = new String[] {ROW_ID, ROW_URL, ROW_TITLE, ROW_FAVICON, ROW_TIMESTAMP};
        Cursor cursor = database.query(true, DB_TABLE, cols, ROW_URL + " = ?", new String[] { url }, null, null, null, null);
        boolean r =(cursor == null || cursor.getCount() > 0);

        if (cursor != null)
            cursor.close();

        return r;
    }

    long updateFavicon(String url, Bitmap bitmap) {
        byte[] favicon = new byte[0];
        if(bitmap != null) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 0, stream);
            favicon = stream.toByteArray();
        }

        ContentValues cv = new  ContentValues();
        cv.put(ROW_FAVICON, favicon);
        long result = database.update(DB_TABLE, cv, ROW_URL + " = ?", new String[] { url });

        this.populateAdapter();

        return result;
    }

    private Cursor getRecords() {
        String[] cols = new String[] {ROW_ID, ROW_URL, ROW_TITLE, ROW_FAVICON, ROW_TIMESTAMP};
        return database.query(true, DB_TABLE, cols, null, null, null, null, ROW_TITLE+" DESC", null);
    }

    public ArrayList<String> getUrls() {
        ArrayList<String> urls = new ArrayList<>();

        Cursor c = this.getRecords();

        while(c.moveToNext())
            urls.add(c.getString(c.getColumnIndex(ROW_URL)));

        c.close();

        return urls;
    }

    private void populateAdapter() {
        arrayList.clear();

        Cursor c = this.getRecords();

        while(c.moveToNext()) {
            byte[] favicon = c.getBlob(c.getColumnIndex(ROW_FAVICON));
            Bitmap bitmap = BitmapFactory.decodeByteArray(favicon, 0, favicon.length);

            arrayList.add(new BookmarksObject(c.getString(c.getColumnIndex(ROW_URL)), c.getString(c.getColumnIndex(ROW_TITLE)), bitmap, c.getLong(c.getColumnIndex(ROW_TIMESTAMP))));
        }

        c.close();

        this.adapter.notifyDataSetChanged();
    }

    Adapter getAdapter() {
        populateAdapter();
        return this.adapter;
    }

    private class BookmarksObject {
        String url;
        String title;
        Bitmap favicon;
        Long visited;

        BookmarksObject(String url, String title, Bitmap favicon, Long visited) {
            this.url = url;
            this.title = title;
            this.favicon = favicon;
            this.visited = visited;
        }
    }

    private class BookmarksHelper extends SQLiteOpenHelper {

        private static final String DATABASE_NAME = "BookmarkManager";
        private static final int DATABASE_VERSION = 2;
        private static final String DATABASE_TABLE = "BookmarkManager";
        private static final String DATABASE_CREATE = "create table " + DATABASE_TABLE + "( _id INTEGER PRIMARY KEY AUTOINCREMENT, url TEXT NOT NULL, title TEXT, favicon BLOB, timestamp NUMERIC DEFAULT CURRENT_TIMESTAMP);";

        BookmarksHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase database) {
            database.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase database,int oldVersion,int newVersion){
            database.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
            onCreate(database);
        }
    }

    private class Adapter extends ArrayAdapter<BookmarksObject> {
        private Activity activity;
        private ArrayList<BookmarksObject> arrayList;
        private LayoutInflater inflater = null;

        Adapter(Activity activity, int textViewResourceId, ArrayList<BookmarksObject> arrayList) {
            super(activity, textViewResourceId, arrayList);
            try {
                this.activity = activity;
                this.arrayList = arrayList;

                inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            } catch (Exception ignored) {
            }
        }

        public int getCount() {
            return arrayList.size();
        }

        public long getItemId(int position) {
            return position;
        }

        class ViewHolder {
            ImageView page_favicon;
            TextView page_url;
            TextView page_title;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            View vi = convertView;
            final ViewHolder holder;
            try {
                if (convertView == null) {
                    vi = inflater.inflate(R.layout.bookmarks_row, null);
                    holder = new ViewHolder();

                    holder.page_favicon = vi.findViewById(R.id.row_favicon);
                    holder.page_title = vi.findViewById(R.id.row_title);
                    holder.page_url = vi.findViewById(R.id.row_url);

                    vi.setTag(holder);
                } else {
                    holder = (ViewHolder) vi.getTag();
                }
                holder.page_favicon.setImageDrawable(new BitmapDrawable(activity.getResources(), arrayList.get(position).favicon));
                holder.page_title.setText(arrayList.get(position).title);
                holder.page_url.setText(arrayList.get(position).url);

                final int pos = position;
                vi.findViewById(R.id.row).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ((WebView)activity.findViewById(R.id.webview)).loadUrl(arrayList.get(pos).url);
                        ((Browser)activity).closeBookmarks(null);
                    }
                });

                vi.findViewById(R.id.row_delete).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        BookmarkManager.this.remove(arrayList.get(pos).url);
                        ((Browser)activity).updateBookmarkButtons();
                    }
                });

            } catch (Exception ignored) {

            }
            return vi;
        }
    }
}