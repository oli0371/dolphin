package org.dolphinemu.dolphinemu.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.dolphinemu.dolphinemu.services.GameFileCacheService;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GameFileCache
{
  // Do not rename or move without editing the native code
  private long mPointer;

  private static final String GAME_FOLDER_PATHS_PREFERENCE = "gameFolderPaths";

  public GameFileCache()
  {
    mPointer = newGameFileCache();
  }

  private static native long newGameFileCache();

  @Override
  public native void finalize();

  public static void addGameFolder(String path, Context context)
  {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
    Set<String> folderPaths =
      preferences.getStringSet(GAME_FOLDER_PATHS_PREFERENCE, new HashSet<>());
    if (!folderPaths.contains(path))
    {
      folderPaths.add(path);
      SharedPreferences.Editor editor = preferences.edit();
      editor.putStringSet(GAME_FOLDER_PATHS_PREFERENCE, folderPaths);
      editor.apply();
    }
  }

  /**
   * Scans through the file system and updates the cache to match.
   *
   * @return true if the cache was modified
   */
  public boolean scanLibrary(Context context)
  {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
    Set<String> folderPathsSet =
      preferences.getStringSet(GAME_FOLDER_PATHS_PREFERENCE, new HashSet<>());

    // get paths from gamefiles
    List<GameFile> gameFiles = GameFileCacheService.getAllGameFiles();
    for (GameFile f : gameFiles)
    {
      String filename = f.getPath();
      int lastSep = filename.lastIndexOf(File.separator);
      if (lastSep > 0)
      {
        String path = filename.substring(0, lastSep);
        if (!folderPathsSet.contains(path))
        {
          folderPathsSet.add(path);
        }
      }
    }

    // remove non exists paths
    for (String p : folderPathsSet)
    {
      File folder = new File(p);
      if (!folder.exists())
      {
        folderPathsSet.remove(p);
      }
    }

    // apply changes
    SharedPreferences.Editor editor = preferences.edit();
    editor.putStringSet(GAME_FOLDER_PATHS_PREFERENCE, folderPathsSet);
    editor.apply();

    String[] folderPaths = folderPathsSet.toArray(new String[0]);
    boolean cacheChanged = update(folderPaths);
    cacheChanged |= updateAdditionalMetadata();
    if (cacheChanged)
    {
      save();
    }
    return cacheChanged;
  }

  public native GameFile[] getAllGames();

  public native GameFile addOrGet(String gamePath);

  private native boolean update(String[] folderPaths);

  private native boolean updateAdditionalMetadata();

  public native boolean load();

  private native boolean save();
}
