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
methods=r"""
    private String editedJob(int dex,String fallback){
        return getSharedPreferences("novaris_job_edits",MODE_PRIVATE).getString(String.valueOf(dex),fallback==null?"":fallback);
    }
    private void addJobEditor(int dex,String current){
        try{
            org.json.JSONObject catalog=loadJobCatalog();
            String[] systems={"Ragnarok Online","Final Fantasy XIV","Civilian","Gatherer","Crafter"};
            android.widget.Spinner systemSpinner=new android.widget.Spinner(this);
            android.widget.Spinner familySpinner=new android.widget.Spinner(this);
            android.widget.Spinner stageSpinner=new android.widget.Spinner(this);
            body.addView(label("Klassenbearbeitung · System",15,true));
            body.addView(systemSpinner);
            body.addView(label("Familie / Laufbahn",14,true));
            body.addView(familySpinner);
            body.addView(label("Klasse / Entwicklungsstufe",14,true));
            body.addView(stageSpinner);
            systemSpinner.setAdapter(new android.widget.ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,systems));
            final java.util.ArrayList<String> familyKeys=new java.util.ArrayList<>();
            final java.util.ArrayList<String> stages=new java.util.ArrayList<>();
            final java.util.ArrayList<String> tiers=new java.util.ArrayList<>();
            final boolean[] initializing={true};
            Runnable refreshStages=()->{
                stages.clear();tiers.clear();
                int system=systemSpinner.getSelectedItemPosition();
                if(familyKeys.isEmpty()||familySpinner.getSelectedItemPosition()<0)return;
                String family=familyKeys.get(Math.min(familySpinner.getSelectedItemPosition(),familyKeys.size()-1));
                String catalogKey=system==0?"V2610_RAG":system==1?"V2622_FFXIV":system==2?"V2610_CIV":system==3?"V2610_GATHER":"V2610_CRAFT";
                org.json.JSONObject entry=catalog.optJSONObject(catalogKey)==null?null:catalog.optJSONObject(catalogKey).optJSONObject(family);
                if(system==0&&entry!=null){
                    org.json.JSONObject tierMap=entry.optJSONObject("tier_map");
                    if(tierMap!=null)for(String tier:new String[]{"T1","T2","Transcendent","T3","T4"}){
                        String job=tierMap.optString(tier,"");
                        if(!job.isEmpty()){stages.add(job);tiers.add(tier);}
                    }
                }else if(system==2&&entry!=null){
                    org.json.JSONArray options=entry.optJSONArray("specializations");
                    if(options!=null)for(int i=0;i<options.length();i++){stages.add(options.optString(i));tiers.add("C"+(i+1));}
                }
                if(stages.isEmpty()){stages.add(family);tiers.add("");}
                stageSpinner.setAdapter(new android.widget.ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new java.util.ArrayList<String>(stages)));
            };
            familySpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
                public void onNothingSelected(android.widget.AdapterView<?> p){}
                public void onItemSelected(android.widget.AdapterView<?> p,android.view.View v,int pos,long id){refreshStages.run();}
            });
            systemSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
                public void onNothingSelected(android.widget.AdapterView<?> p){}
                public void onItemSelected(android.widget.AdapterView<?> p,android.view.View v,int pos,long id){
                    familyKeys.clear();
                    String key=pos==0?"V2610_RAG":pos==1?"V2622_FFXIV":pos==2?"V2610_CIV":pos==3?"V2610_GATHER":"V2610_CRAFT";
                    org.json.JSONObject options=catalog.optJSONObject(key);
                    if(options!=null){java.util.Iterator<String> iterator=options.keys();while(iterator.hasNext())familyKeys.add(iterator.next());}
                    java.util.Collections.sort(familyKeys);
                    familySpinner.setAdapter(new android.widget.ArrayAdapter<String>(DetailActivity.this,android.R.layout.simple_spinner_dropdown_item,new java.util.ArrayList<String>(familyKeys)));
                    refreshStages.run();
                }
            });
            Button save=button("Ausgewählte Klasse speichern");
            body.addView(save);
            save.setOnClickListener(v->{
                int sys=systemSpinner.getSelectedItemPosition();
                int fi=familySpinner.getSelectedItemPosition();
                int si=stageSpinner.getSelectedItemPosition();
                if(fi<0||si<0||fi>=familyKeys.size()||si>=stages.size())return;
                saveStructuredJob(dex,stages.get(si),systems[sys],familyKeys.get(fi),tiers.get(si),sys==2?stages.get(si):"");
            });
            Button transfer=button("Klassenänderungen importieren / exportieren");
            body.addView(transfer);
            transfer.setOnClickListener(v->new AlertDialog.Builder(this)
                .setItems(new String[]{"JSON importieren","JSON exportieren"},(d,i)->{if(i==0)importJobEdits();else exportJobEdits();})
                .show());
        }catch(Exception e){Toast.makeText(this,"Klasseneditor nicht verfügbar: "+e.getMessage(),Toast.LENGTH_LONG).show();}
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
            getSharedPreferences("novaris_job_manual",MODE_PRIVATE).edit().putBoolean(String.valueOf(dex),true).commit();
            propagateJobForward(dex,record);
            saveSelectedJob(dex,selected);
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
        android.content.SharedPreferences.Editor descEditor=getSharedPreferences("novaris_job_descriptions",MODE_PRIVATE).edit();
        for(int i=0;i<assignments.length();i++){
            org.json.JSONObject row=assignments.getJSONObject(i);
            org.json.JSONObject selected=row.getJSONObject("selected");
            String description=selected.optString("description","");
            if(description.length()>100000)throw new org.json.JSONException("Beschreibung zu lang");
            if(!description.isEmpty())descEditor.putString(String.valueOf(row.getInt("dex")),description);
        }
        if(!descEditor.commit())throw new java.io.IOException("Beschreibungen nicht gespeichert");
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
                selected.put("description",getSharedPreferences("novaris_job_descriptions",MODE_PRIVATE).getString(dexKey,""));
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
        org.json.JSONObject entries=catalog.optJSONObject("Ragnarok Online".equals(group)?"V2610_RAG":"Civilian".equals(group)?"V2610_CIV":"V2622_FFXIV");
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
                    int stage=Math.min(stages.length()-1,Math.max(0,i-position));
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
    private String generateClassDescription(DatabaseManager.PokemonItem item,String changed){
        try{
            String key=String.valueOf(item.dex);
            String saved=getSharedPreferences("novaris_job_descriptions",MODE_PRIVATE).getString(key,"");
            if(!saved.isEmpty())return saved;
            String raw=getSharedPreferences("novaris_job_structured",MODE_PRIVATE).getString(key,"");
            if(raw.isEmpty())return "";
            org.json.JSONObject record=new org.json.JSONObject(raw);
            String group=record.optString("group","");
            String family=record.optString("class_family","");
            String tier=record.optString("tier","");
            org.json.JSONObject catalog=loadJobCatalog();
            String catalogKey="Ragnarok Online".equals(group)?"V2610_RAG":"Civilian".equals(group)?"V2610_CIV":"Final Fantasy XIV".equals(group)?"V2622_FFXIV":"Gatherer".equals(group)?"V2610_GATHER":"V2610_CRAFT";
            org.json.JSONObject entry=catalog.optJSONObject(catalogKey)==null?null:catalog.optJSONObject(catalogKey).optJSONObject(family);
            String purpose=entry==null?"":entry.optString("desc_de",entry.optString("description_de",""));
            if(purpose.isEmpty())purpose="Berufliche Tätigkeiten und Aufgaben der Klasse "+changed+".";
            String source=data.buildPokemonPrompt(item.dex,false);
            int start=source.indexOf("SPECIES IDENTITY:");
            int end=source.indexOf("NOVARIS LIFE ROLE:");
            if(start<0||end<=start)return "";
            String identity=source.substring(start,end).trim();
            String description="#"+String.format(java.util.Locale.ROOT,"%04d",item.dex)+"."+item.en+" / "+item.de+" — "+changed+"\n\n"+
                identity+"\n\nNOVARIS LIFE ROLE:\nCATEGORY: "+group+"\nROLE SYSTEM: "+group+
                "\nCLASS FAMILY: "+family+"\nCURRENT CLASS: "+changed+"\nCLASS STATUS: "+tier+
                "\nROLE PURPOSE: "+purpose+"\n\nCHARACTER BLUEPRINT:\n"+
                "IDENTITY: "+item.en+"; human adult interpretation retaining the original Pokémon colors, motifs, temperament and recognizable species traits.\n"+
                "STATURE AND SILHOUETTE: Follow the original species and evolution stage; normal adult human anatomy only.\n"+
                "EXPRESSION AND POSE: Show the distinctive responsibilities of "+changed+" through stance, tools and environmental interaction.\n"+
                "SPECIES FEATURES: Any nonhuman wings, tails, shells or appendages must be detachable garments or equipment, never anatomy.\n\n"+
                "WORK OUTFIT BLUEPRINT:\n"+
                "ROLE: "+changed+".\n"+
                "OUTFIT: Purpose-built professional clothing for "+purpose+"; incorporate recognizable "+item.en+" colors and motifs.\n"+
                "TOOLS: External handheld or wearable equipment appropriate for "+changed+"; no weapons or machinery emerging from flesh.\n"+
                "ENVIRONMENT: A credible workplace or activity scene showing "+purpose+"\n\n"+
                "NOVARIS OUTPUT CONTROL:\n"+
                "Create one full-body adult human character, exactly two arms, two legs, one head and one torso. "+
                "Preserve the species identity through color, motifs, fashion, props and lighting. "+
                "No biological extra anatomy. One coherent character, detailed professional costume and context.\n";
            return description;
        }catch(Exception e){android.util.Log.e("NovarisJobs","Description generation failed",e);return "";}
    }
    private void addChangedJobBlock(DatabaseManager.PokemonItem item){
        String changed=editedJob(item.dex,item.role);
        if(changed.equals(item.role))return;
        String description=generateClassDescription(item,changed);
        section("GEÄNDERTE KLASSE · "+changed,description.isEmpty()?"Beschreibung ausstehend. Bitte vollständigen neuen Rollen-, Charakter- und Outfit-Text per JSON importieren. Original unverändert.":description);
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
