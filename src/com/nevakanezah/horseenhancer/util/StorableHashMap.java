package com.nevakanezah.horseenhancer.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;

import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

/**
 * 
 * @author Nevakanezah
 * List manager for the list of HorseData objects. Handles file I/O, and some management operations.
 * Adapted from StorableHashMap written by CleSurrealism - https://bukkit.org/threads/storablehashmap-using-serialisation.389522
 *
 * @param <K> The object
 * @param <V> The value
 */
public class StorableHashMap<K, V> extends HashMap<K, V> implements Serializable
{
    private static final long serialVersionUID = -3535879180214628774L;

    private static final transient String FILE_EXTENSION = ".dat";

    private transient File saveFile;

    public StorableHashMap(File parentFolder, String fileName) throws IOException
    {
        super();
        initialise(parentFolder, fileName);
    }

    public StorableHashMap(int initialCapacity, File parentFolder, String fileName) throws IOException
    {
        super(initialCapacity);
        initialise(parentFolder, fileName);
    }

    public StorableHashMap(int initialCapacity, float loadFactor, File parentFolder, String fileName) throws IOException
    {
        super(initialCapacity, loadFactor);
        initialise(parentFolder, fileName);
    }

    public StorableHashMap(HashMap<K, V> m, File parentFolder, String fileName) throws IOException
    {
        super(m);
        initialise(parentFolder, fileName);
    }

    private void initialise(File parentFolder, String fileName) throws IOException
    {
        this.saveFile = new File(parentFolder, fileName + FILE_EXTENSION);
        
    }

    @SuppressWarnings("unchecked")
    public void loadFromFile() throws IOException, ClassNotFoundException
    {
        if (saveFile.length() > 0)
        {
            try (FileInputStream fileIn = new FileInputStream(saveFile);
                    BukkitObjectInputStream objectIn = new BukkitObjectInputStream(fileIn))
            {
                StorableHashMap<K, V> map = (StorableHashMap<K, V>) objectIn.readObject();
                super.clear();
                super.putAll(map);
            }
        }
    }

    public void saveToFile() throws IOException
    {
        try (FileOutputStream fileOut = new FileOutputStream(saveFile);
                BukkitObjectOutputStream objectOut = new BukkitObjectOutputStream(fileOut))
        {
            objectOut.writeObject(this);
        }
    }
}