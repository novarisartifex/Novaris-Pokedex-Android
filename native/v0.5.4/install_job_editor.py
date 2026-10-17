# Versioned source migration from the existing native APK workflow.
from pathlib import Path
p=Path("appsrc/app/src/main/java/com/novaris/pokedex/nativeapp/DetailActivity.java")
s=p.read_text()
import re
pattern=r'(?m)^(\s*)addFormCards\('
if not re.search(pattern,s):raise SystemExit("Job editor UI insertion point missing")
s=re.sub(pattern,lambda m:m.group(1)+'addJobEditor(d.item.dex,d.item.role);\n'+m.group(1)+'addChangedJobBlock(d.item);\n'+m.group(1)+'addFormCards(',s,count=1)
anchor='private void copy(String s){'
if anchor not in s:raise SystemExit("Job editor anchor missing")
methods="""
    private String editedJob(int dex,String fallback){
        return getSharedPreferences("novaris_job_edits",MODE_PRIVATE).getString(String.valueOf(dex),fallback==null?"":fallback);
    }
    private void addJobEditor(int dex,String current){
        Button edit=button("Job-Zuordnung auswählen / Export");
        body.addView(edit);
        edit.setOnClickListener(v->openJobCatalog(dex,current));
    }
    private void openJobCatalog(int dex,String current){
        try{
            java.io.InputStream input=getAssets().open("novaris_job_catalog_v2742.json");
            java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();
            byte[] buffer=new byte[8192];int count;
            while((count=input.read(buffer))!=-1)bytes.write(buffer,0,count);
            input.close();
            org.json.JSONObject catalog=new org.json.JSONObject(bytes.toString("UTF-8"));
            String[] systems={"Ragnarok Online","Final Fantasy XIV","Civilian","Gatherer","Crafter"};
            new AlertDialog.Builder(this).setTitle("Grundklasse / Berufsgruppe")
              .setItems(systems,(dialog,system)->{
                  if(system==0||system==2)showJobFamilies(dex,current,catalog,system);
                  else showFlatJobCatalog(dex,catalog,system);
              })
              .setNeutralButton("Import / Export",(dialog,which)->new AlertDialog.Builder(this)
                  .setItems(new String[]{"JSON importieren","JSON exportieren"},(d2,choice)->{
                      if(choice==0)importJobEdits();else exportJobEdits();
                  }).show())
              .setNegativeButton("Abbrechen",null).show();
        }catch(Exception e){Toast.makeText(this,"Jobkatalog nicht verfügbar: "+e.getMessage(),Toast.LENGTH_LONG).show();}
    }
    private void showFlatJobCatalog(int dex,org.json.JSONObject catalog,int system){
        try{
            String key=system==1?"V2622_FFXIV":system==3?"V2610_GATHER":"V2610_CRAFT";
            org.json.JSONObject jobs=catalog.getJSONObject(key);
            java.util.ArrayList<String> names=new java.util.ArrayList<>();
            java.util.Iterator<String> it=jobs.keys();
            while(it.hasNext())names.add(it.next());
            java.util.Collections.sort(names);
            new AlertDialog.Builder(this).setTitle("Klasse / Beruf")
              .setItems(names.toArray(new String[0]),(dialog,index)->saveStructuredJob(dex,names.get(index),system==1?"Final Fantasy XIV":system==3?"Gatherer":"Crafter","", "", ""))
              .setNegativeButton("Abbrechen",null).show();
        }catch(Exception e){Toast.makeText(this,"Jobauswahl fehlgeschlagen: "+e.getMessage(),Toast.LENGTH_LONG).show();}
    }
    private void saveStructuredJob(int dex,String selected,String group,String family,String tier,String specialization){
        try{
            org.json.JSONObject record=new org.json.JSONObject();
            record.put("label",selected);
            record.put("role",selected);
            record.put("group",group);
            record.put("class_family",family);
            record.put("tier",tier);
            record.put("specialization",specialization);
            String prefix="Ragnarok Online".equals(group)?"ro:":"Civilian".equals(group)?"civil:":"Final Fantasy XIV".equals(group)?"ffxiv:":"Gatherer".equals(group)?"gather:":"craft:";
            record.put("id",prefix+("Civilian".equals(group)?family:selected));
            record.put("root_key","Ragnarok Online".equals(group)?"ro-root:"+family.split(" → ")[0]:"group:"+("Final Fantasy XIV".equals(group)?"FFXIV":group));
            boolean structuredSaved=getSharedPreferences("novaris_job_structured",MODE_PRIVATE).edit().putString(String.valueOf(dex),record.toString()).commit();
            if(!structuredSaved)throw new java.io.IOException("Strukturierte Jobdaten nicht gespeichert");
            getSharedPreferences("novaris_job_manual",MODE_PRIVATE).edit().putBoolean(String.valueOf(dex),true).commit();\n            propagateJobForward(dex,record);\n            saveSelectedJob(dex,selected);
        }catch(Exception e){Toast.makeText(this,"Job nicht gespeichert: "+e.getMessage(),Toast.LENGTH_LONG).show();}
    }
    private void saveSelectedJob(int dex,String selected){
        if(!getSharedPreferences("novaris_job_edits",MODE_PRIVATE).edit().putString(String.valueOf(dex),selected).commit()){
            Toast.makeText(this,"Job konnte nicht gespeichert werden",Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(this,"Job lokal gespeichert: "+selected,Toast.LENGTH_SHORT).show();
        body.removeAllViews();showPokemon(dex);
    }
    private void showJobFamilies(int dex,String current,org.json.JSONObject catalog,int system){
        try{
            org.json.JSONObject families=catalog.getJSONObject(system==0?"V2610_RAG":"V2610_CIV");
            java.util.ArrayList<String> names=new java.util.ArrayList<>();
            java.util.Iterator<String> it=families.keys();
            while(it.hasNext())names.add(it.next());
            java.util.Collections.sort(names);
            if(system==0){
                java.util.ArrayList<String> roots=new java.util.ArrayList<>();
                for(String family:names){
                    org.json.JSONObject item=families.optJSONObject(family);
                    String root=item==null?"":item.optString("root","");
                    if(!root.isEmpty()&&!roots.contains(root))roots.add(root);
                }
                java.util.Collections.sort(roots);
                new AlertDialog.Builder(this).setTitle("Ragnarok Grundklasse wählen")
                  .setItems(roots.toArray(new String[0]),(dialog,index)->{
                      java.util.ArrayList<String> branches=new java.util.ArrayList<>();
                      for(String family:names){
                          org.json.JSONObject item=families.optJSONObject(family);
                          if(item!=null&&roots.get(index).equals(item.optString("root","")))branches.add(family);
                      }
                      new AlertDialog.Builder(this).setTitle("Klassenlaufbahn wählen")
                        .setItems(branches.toArray(new String[0]),(d2,i)->showJobStages(dex,current,families,branches.get(i),system))
                        .setNegativeButton("Abbrechen",null).show();
                  }).setNegativeButton("Abbrechen",null).show();
            }else{
                new AlertDialog.Builder(this).setTitle("Berufsfamilie auswählen")
                  .setItems(names.toArray(new String[0]),(dialog,index)->showJobStages(dex,current,families,names.get(index),system))
                  .setNegativeButton("Abbrechen",null).show();
            }
        }catch(Exception e){Toast.makeText(this,"Familienauswahl fehlgeschlagen",Toast.LENGTH_LONG).show();}
    }
    private void showJobStages(int dex,String current,org.json.JSONObject families,String family,int system){
        try{
            org.json.JSONObject entry=families.optJSONObject(family);
            java.util.ArrayList<String> stages=new java.util.ArrayList<>();
            if(system==2 && entry!=null){
                org.json.JSONArray a=entry.optJSONArray("specializations");
                if(a!=null)for(int i=0;i<a.length();i++)stages.add("C"+(i+1)+" · "+a.optString(i));
            }else if(entry!=null){
                org.json.JSONObject tierMap=entry.optJSONObject("tier_map");
                if(tierMap!=null){
                    String[] tiers={"T1","T2","Transcendent","T3","T4"};
                    for(String tier:tiers){
                        String name=tierMap.optString(tier,"");
                        if(!name.isEmpty())stages.add(name);
                    }
                }
            }
            if(stages.isEmpty())stages.add(family);
            new AlertDialog.Builder(this).setTitle(family+" – Stufe auswählen")
              .setItems(stages.toArray(new String[0]),(dialog,index)->{
                  String selected=stages.get(index);
                  if(system==2&&selected.startsWith("C")){
                      int separator=selected.indexOf(" · ");
                      if(separator>=0)selected=selected.substring(separator+3);
                  }
                  String tier=system==2?"C"+(index+1):"";
                  if(system==0&&entry!=null){
                      org.json.JSONObject tierMap=entry.optJSONObject("tier_map");
                      if(tierMap!=null){
                          java.util.Iterator<String> keys=tierMap.keys();
                          while(keys.hasNext()){String key=keys.next();if(selected.equals(tierMap.optString(key)))tier=key;}
                      }
                  }
                  saveStructuredJob(dex,selected,system==0?"Ragnarok Online":"Civilian",family,tier,system==2?selected:"");
              }).setNegativeButton("Abbrechen",null).show();
        }catch(Exception e){Toast.makeText(this,"Stufenauswahl fehlgeschlagen",Toast.LENGTH_LONG).show();}
    }
    private org.json.JSONObject loadJobBaseline()throws Exception{
        java.io.InputStream input=getAssets().open("novaris_job_baseline_v2742.json");
        java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();
        byte[] buffer=new byte[8192];int count;
        while((count=input.read(buffer))!=-1)bytes.write(buffer,0,count);
        input.close();
        org.json.JSONArray rows=new org.json.JSONArray(bytes.toString("UTF-8"));
        org.json.JSONObject byDex=new org.json.JSONObject();
        for(int i=0;i<rows.length();i++){
            org.json.JSONObject row=rows.getJSONObject(i);
            byDex.put(String.valueOf(row.getInt("dex")),row);
        }
        return byDex;
    }
    private void importJobEdits(){
        android.content.Intent intent=new android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(android.content.Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        startActivityForResult(intent,9913);
    }
    private void readJobEdits(android.net.Uri uri)throws Exception{
        java.io.InputStream input=getContentResolver().openInputStream(uri);
        if(input==null)throw new java.io.IOException("Datei nicht lesbar");
        java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();
        byte[] buffer=new byte[8192];int n;
        try(java.io.InputStream stream=input){
            while((n=stream.read(buffer))!=-1){
                if(bytes.size()+n>4000000)throw new java.io.IOException("Datei zu groß");
                bytes.write(buffer,0,n);
            }
        }
        org.json.JSONObject root=new org.json.JSONObject(bytes.toString("UTF-8"));
        if(!"NOVARIS_JOB_ASSIGNMENTS_v16".equals(root.optString("format")))
            throw new org.json.JSONException("Unbekanntes Zuordnungsformat");
        org.json.JSONArray assignments=root.getJSONArray("assignments");
        if(root.has("assignmentCount")&&root.getInt("assignmentCount")!=assignments.length())
            throw new org.json.JSONException("Zuordnungsanzahl stimmt nicht überein");
        android.content.SharedPreferences.Editor editor=getSharedPreferences("novaris_job_edits",MODE_PRIVATE).edit();
        java.util.HashMap<String,String> validated=new java.util.HashMap<>();
        java.util.HashMap<String,String> structuredValidated=new java.util.HashMap<>();
        org.json.JSONObject baseline=loadJobBaseline();
        int imported=0;
        for(int i=0;i<assignments.length();i++){
            org.json.JSONObject item=assignments.getJSONObject(i);
            int dex=item.getInt("dex");
            if(dex<1||dex>1025)throw new org.json.JSONException("Ungültige Dex-ID");
            if(!baseline.has(String.valueOf(dex)))throw new org.json.JSONException("Dex-ID nicht im Originalkatalog: "+dex);
            if(validated.containsKey(String.valueOf(dex)))throw new org.json.JSONException("Doppelte Dex-ID: "+dex);
            org.json.JSONObject selected=item.getJSONObject("selected");
            String role=selected.optString("role",selected.optString("label","")).trim();
            if(role.isEmpty())throw new org.json.JSONException("Leere Jobzuordnung für Dex "+dex);
            String label=selected.optString("label","").trim();
            if(!label.isEmpty()&&!label.equals(role))throw new org.json.JSONException("Widersprüchliche Jobbezeichnung für Dex "+dex);
            if(role.length()>250)throw new org.json.JSONException("Jobbezeichnung zu lang für Dex "+dex);
            validated.put(String.valueOf(dex),role);
            structuredValidated.put(String.valueOf(dex),selected.toString());
            imported++;
        }
        android.content.SharedPreferences flatPrefs=getSharedPreferences("novaris_job_edits",MODE_PRIVATE);
        android.content.SharedPreferences structuredPrefs=getSharedPreferences("novaris_job_structured",MODE_PRIVATE);
        java.util.Map<String,?> oldStructured=structuredPrefs.getAll();
        android.content.SharedPreferences.Editor structuredEditor=structuredPrefs.edit();
        for(java.util.Map.Entry<String,String> entry:structuredValidated.entrySet())
            structuredEditor.putString(entry.getKey(),entry.getValue());
        if(!structuredEditor.commit())throw new java.io.IOException("Strukturierte Daten konnten nicht gespeichert werden");
        for(java.util.Map.Entry<String,String> entry:validated.entrySet())
            editor.putString(entry.getKey(),entry.getValue());
        if(!editor.commit()){
            android.content.SharedPreferences.Editor rollback=structuredPrefs.edit().clear();
            for(java.util.Map.Entry<String,?> entry:oldStructured.entrySet())
                if(entry.getValue() instanceof String)rollback.putString(entry.getKey(),(String)entry.getValue());
            rollback.commit();
            throw new java.io.IOException("Lokales Speichern fehlgeschlagen; strukturierte Daten zurückgesetzt");
        }
        Toast.makeText(this,imported+" Job-Zuordnungen importiert",Toast.LENGTH_LONG).show();
        recreate();
    }
    private void exportJobEdits(){
        android.content.Intent intent=new android.content.Intent(android.content.Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(android.content.Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(android.content.Intent.EXTRA_TITLE,"novaris_job_aenderungen.json");
        startActivityForResult(intent,9912);
    }
    @Override protected void onActivityResult(int request,int result,android.content.Intent dataIntent){
        super.onActivityResult(request,result,dataIntent);
        if(result!=RESULT_OK||dataIntent==null||dataIntent.getData()==null)return;
        if(request==9913){
            try{readJobEdits(dataIntent.getData());}
            catch(Exception e){Toast.makeText(this,"Import fehlgeschlagen: "+e.getMessage(),Toast.LENGTH_LONG).show();}
            return;
        }
        if(request!=9912)return;
        try{
            org.json.JSONObject root=new org.json.JSONObject();
            root.put("format","NOVARIS_JOB_ASSIGNMENTS_v16");
            root.put("pokedexVersion","V27.0");
            org.json.JSONArray assignments=new org.json.JSONArray();
            org.json.JSONObject baseline=loadJobBaseline();
            java.util.Map<String,?> edits=getSharedPreferences("novaris_job_edits",MODE_PRIVATE).getAll();
            java.util.ArrayList<String> sortedDexes=new java.util.ArrayList<>(edits.keySet());
            for(String key:sortedDexes){
                int number=Integer.parseInt(key);
                if(number<1||number>1025||!baseline.has(key))throw new org.json.JSONException("Ungültige lokale Dex-ID: "+key);
            }
            java.util.Collections.sort(sortedDexes,(a,b)->Integer.compare(Integer.parseInt(a),Integer.parseInt(b)));
            for(String dexKey:sortedDexes){
                Object editedRole=edits.get(dexKey);
                org.json.JSONObject original=baseline.optJSONObject(dexKey);
                org.json.JSONObject item=new org.json.JSONObject();
                item.put("dex",Integer.parseInt(dexKey));
                item.put("original",original==null?new org.json.JSONObject():original);
                org.json.JSONObject selected=new org.json.JSONObject();
                String structured=getSharedPreferences("novaris_job_structured",MODE_PRIVATE).getString(dexKey,null);
                if(structured!=null)selected=new org.json.JSONObject(structured);
                selected.put("role",String.valueOf(editedRole));
                selected.put("label",String.valueOf(editedRole));
                item.put("selected",selected);
                assignments.put(item);
            }
            root.put("assignments",assignments);
            root.put("assignmentCount",assignments.length());
            try(java.io.OutputStream output=getContentResolver().openOutputStream(dataIntent.getData())){
                output.write(root.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            Toast.makeText(this,"Job-Änderungen exportiert",Toast.LENGTH_LONG).show();
        }catch(Exception e){Toast.makeText(this,"Export fehlgeschlagen: "+e.getMessage(),Toast.LENGTH_LONG).show();}
    }
    private void propagateJobForward(int dex,org.json.JSONObject selected)throws Exception{
        org.json.JSONObject baseline=loadJobBaseline();
        org.json.JSONObject origin=baseline.optJSONObject(String.valueOf(dex));
        if(origin==null)return;
        String evo=origin.optString("evolution","");
        if(evo.isEmpty())return;
        java.util.regex.Matcher matcher=java.util.regex.Pattern.compile("#([0-9]{4})[.]").matcher(evo);
        java.util.ArrayList<Integer> chain=new java.util.ArrayList<>();
        while(matcher.find()){
            int id=Integer.parseInt(matcher.group(1));
            if(!chain.contains(id))chain.add(id);
        }
        int position=chain.indexOf(dex);
        if(position<0)return;
        String group=selected.optString("group","");
        String family=selected.optString("class_family","");
        org.json.JSONObject catalog=loadJobCatalog();
        org.json.JSONObject entries=catalog.optJSONObject("Ragnarok Online".equals(group)?"V2610_RAG":"Civilian".equals(group)?"V2610_CIV":"");
        org.json.JSONObject familyEntry=entries==null?null:entries.optJSONObject(family);
        for(int i=position+1;i<chain.size();i++){
            int nextDex=chain.get(i);
            String key=String.valueOf(nextDex);
            if(getSharedPreferences("novaris_job_manual",MODE_PRIVATE).getBoolean(key,false))continue;
            org.json.JSONObject original=baseline.optJSONObject(key);
            if(original==null)continue;
            String role=selected.optString("role","");
            String tier=original.optString("tier","");
            if("Ragnarok Online".equals(group)&&familyEntry!=null){
                org.json.JSONObject tierMap=familyEntry.optJSONObject("tier_map");
                if(tierMap!=null){
                    role=tierMap.optString(tier,"");
                    if(role.isEmpty())role=selected.optString("role","");
                }
            }else if("Civilian".equals(group)&&familyEntry!=null){
                org.json.JSONArray stages=familyEntry.optJSONArray("specializations");
                if(stages!=null&&stages.length()>0){
                    int stage=Math.min(stages.length()-1,Math.max(0,i));
                    role=stages.optString(stage,role);
                    tier="C"+(stage+1);
                }
            }
            org.json.JSONObject child=new org.json.JSONObject(selected.toString());
            child.put("role",role);child.put("label",role);child.put("tier",tier);
            child.put("propagated_from",dex);
            getSharedPreferences("novaris_job_edits",MODE_PRIVATE).edit().putString(key,role).commit();
            getSharedPreferences("novaris_job_structured",MODE_PRIVATE).edit().putString(key,child.toString()).commit();
        }
    }
    private org.json.JSONObject loadJobCatalog()throws Exception{
        java.io.InputStream input=getAssets().open("novaris_job_catalog_v2742.json");
        java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();
        byte[] buffer=new byte[8192];int count;
        try(java.io.InputStream stream=input){while((count=stream.read(buffer))!=-1)bytes.write(buffer,0,count);}
        return new org.json.JSONObject(bytes.toString("UTF-8"));
    }
    private void addChangedJobBlock(DatabaseManager.PokemonItem item){
        String changed=editedJob(item.dex,item.role);
        if(changed.equals(item.role))return;
        section("GEÄNDERTE KLASSE · "+changed,"Beschreibung ausstehend. Der vollständige neue Rollen-, Charakter- und Outfit-Text muss vor der Bildgenerierung importiert werden. Der Originalblock bleibt unverändert.");
        Button img=button("Copy image · geänderte Klasse");
        img.setOnClickListener(v->ImageClipboard.copy(this,item.art));
        body.addView(img);
        String file=String.format(java.util.Locale.ROOT,"#%04d.%s_00_Main_%s",item.dex,item.en,changed.replaceAll("[^A-Za-z0-9]+","_"));
        Button fn=button(file);
        fn.setOnClickListener(v->copy(file));
        body.addView(fn);
        LinearLayout row=new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        Button x1=button("Copy Core + Dex x1");
        Button x3=button("Copy Core + Dex x3");
        x1.setEnabled(false);
        x3.setEnabled(false);
        row.addView(x1,new LinearLayout.LayoutParams(0,dp(62),1));
        row.addView(x3,new LinearLayout.LayoutParams(0,dp(62),1));
        body.addView(row);
    }
    private String applyEditedJob(int dex,String prompt,String original){
        String edited=editedJob(dex,original);
        if(prompt==null||original==null||original.isEmpty()||edited.equals(original))return prompt;
        return prompt.replace(original,edited);
    }
    """
s=s.replace(anchor,methods+'\n'+anchor,1)


p.write_text(s)
assert 'NOVARIS_JOB_ASSIGNMENTS_v16' in s
