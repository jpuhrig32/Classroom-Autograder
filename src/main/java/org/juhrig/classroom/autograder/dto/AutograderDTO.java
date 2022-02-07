package org.juhrig.classroom.autograder.dto;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import java.io.Reader;

public abstract class AutograderDTO {

   private Class clazz;
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

   public AutograderDTO(Class clazz){
       this.clazz = clazz;
   }
    public AutograderDTO loadFromJson(Reader jsonReader){
       return (AutograderDTO) GSON.fromJson(jsonReader, clazz);
    }

    public AutograderDTO loadFromJson(String jsonString){
       return (AutograderDTO)GSON.fromJson(jsonString, clazz);
    }

    public JsonElement toJsonElement(){
       return GSON.toJsonTree(this);
    }

    public String toJson(){
       return GSON.toJson(this);
    }
}
