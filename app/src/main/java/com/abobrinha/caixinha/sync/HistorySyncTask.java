package com.abobrinha.caixinha.sync;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import com.abobrinha.caixinha.R;
import com.abobrinha.caixinha.data.HistoryContract;
import com.abobrinha.caixinha.network.WordPressConn;
import com.abobrinha.caixinha.network.WordPressJson;

import org.json.JSONException;

import java.io.IOException;


public class HistorySyncTask {

    public static final int HISTORY_STATUS_OK = 0;
    public static final int HISTORY_STATUS_SERVER_DOWN = 1;
    public static final int
            HISTORY_STATUS_SERVER_INVALID = 2;
    public static final int
            HISTORY_STATUS_UNKNOWN = 3;

    /*
     *  Essa rotina sincroniza a base de dados com a API utilizando a seguinte estratégia:
     * 1) Guardar os ID's das histórias marcadas como favoritas
     * 2) Deletar todas as histórias da base
     * 3) Recriar toda a base incluindo novas histórias
     * 4) Restaurar as marcações prévias de favoritos
     * 5) Indicar quantidade de novas histórias     */
    synchronized public static void syncHistories(Context context) {
        try {
            Log.v("SYNC_HIST", "Sincronizando...");

            setHistoryStatus(context, HISTORY_STATUS_UNKNOWN);

            String wordPressSearchResults = WordPressConn.getResponseFromAPI();
            if (wordPressSearchResults == null) {
                setHistoryStatus(context, HISTORY_STATUS_SERVER_DOWN);
                return;
            }

            ContentValues[] historiesValues =
                    WordPressJson.getHistoriesFromJson(context, wordPressSearchResults);

            Uri allHistoriesUri = HistoryContract.HistoriesEntry.CONTENT_URI;
            Uri favoritesUri = HistoryContract.HistoriesEntry.buildFavoritesUri();

            String[] favoritesSaved = null;

            String[] projection = new String[]{HistoryContract.HistoriesEntry._ID};
            Cursor cursor = context.getContentResolver()
                    .query(favoritesUri, projection, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                int i = 0;
                favoritesSaved = new String[cursor.getCount()];
                do {
                    favoritesSaved[i++] = cursor.getString(0);
                } while (cursor.moveToNext());
            }
            cursor.close();

            int oldHistoryQuantity = context.getContentResolver()
                    .delete(allHistoriesUri, null, null);

            int newHistoryQuantity = context.getContentResolver()
                    .bulkInsert(allHistoriesUri, historiesValues);

            if (favoritesSaved != null) {
                context.getContentResolver().update(favoritesUri, null, null, favoritesSaved);
            }

            if (oldHistoryQuantity > 0 && newHistoryQuantity > oldHistoryQuantity) {
                int newHistories = newHistoryQuantity - oldHistoryQuantity;
                switch (newHistories) {
                    case 1:
                        Log.v("SYNC_HIST", "1 nova história adicionada");

                        // ToDo: Implementar notificação para a história nova
                        break;
                    default:
                        Log.v("SYNC_HIST", newHistories + " novas histórias adicionadas");

                        // ToDo: Implementar notificação indicando "x" novas histórias
                        break;
                }
            }

            setHistoryStatus(context, HISTORY_STATUS_OK);

        } catch (IOException e) {
            setHistoryStatus(context, HISTORY_STATUS_SERVER_DOWN);
        } catch (JSONException e) {
            setHistoryStatus(context, HISTORY_STATUS_SERVER_INVALID);
        }
    }

    private static void setHistoryStatus(Context c, int historyStatus) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        SharedPreferences.Editor spe = sp.edit();
        spe.putInt(c.getString(R.string.pref_history_status_key), historyStatus);
        spe.commit();
    }
}
