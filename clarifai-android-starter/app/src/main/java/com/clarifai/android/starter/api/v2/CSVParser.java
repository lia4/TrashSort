package com.clarifai.android.starter.api.v2;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Map;
import java.util.HashMap;

/**
 * Created by fkuang on 10/16/2016.
 */

public class CSVParser {

    public class Tuple {
        String category;
        String tags;
        public Tuple(String category_p, String tag_p){
            category = category_p;
            tags = tag_p;
        }
    }

    public Map<String, Tuple> read() throws FileNotFoundException{
        Scanner scan = new Scanner(new File("../../../../../../../items/items.csv"));
        scan.nextLine();
        Map<String, Tuple> res = new HashMap<>();
        while(scan.hasNextLine()){
            String token = scan.nextLine();
            String[] splitted = token.split(",");
            res.put(splitted[0], new Tuple(splitted[1], splitted[2]));
        }
        return res;
    }

    public Map<String, String> readMapString() throws FileNotFoundException{
        Scanner scan = new Scanner(new File("../../../../../../../items/items.csv"));
        scan.nextLine();
        Map<String, String> res = new HashMap<>();
        while(scan.hasNextLine()){
            String token = scan.nextLine();
            String[] splitted = token.split(",");
            res.put(splitted[2], splitted[1]);
        }
        return res;
    }

}
