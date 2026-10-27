from pathlib import Path

source = Path("appsrc/app/src/main/java/com/novaris/pokedex/nativeapp/DetailActivity.java")
s = source.read_text()
old = 'x3.setOnClickListener(v->copyTriple(originalVariantPrompt(dex,variant,lifestyle,true,root)));'
new = 'x3.setOnClickListener(v->{if(lifestyle && dex>=1 && dex<=151)showGen1X3(dex,variant,root);else copyTriple(originalVariantPrompt(dex,variant,lifestyle,true,root));});'
assert s.count(old)==1, "Expected one Gen1 lifestyle x3 listener"
s=s.replace(old,new,1)
anchor='private void copyTriple(String s){'
assert s.count(anchor)==1, "Copy helper insertion point missing"
method='''private void showGen1X3(int dex,String variant,JSONObject root){
        try{
            final String[] prompts=Gen1TriplePrompts.build(this,root,dex,variant);
            String[] choices={"Bild 1 kopieren","Bild 2 kopieren","Bild 3 kopieren"};
            new android.app.AlertDialog.Builder(this)
                .setTitle(variant+" x3 — 3 einzelne Prompts")
                .setMessage("Jedes Bild separat kopieren und einzeln generieren. Die App startet keine Bildgenerierung.")
                .setItems(choices,(dialog,index)->{
                    copy(prompts[index]);
                    android.widget.Toast.makeText(this,"Bild "+(index+1)+" kopiert",android.widget.Toast.LENGTH_SHORT).show();
                    showGen1X3(dex,variant,root);
                })
                .setNegativeButton("Schließen",null).show();
        }catch(Exception e){
            new android.app.AlertDialog.Builder(this).setTitle("x3-Prompts fehlgeschlagen")
                .setMessage(e.getMessage()).setPositiveButton("OK",null).show();
        }
    }
    '''
s=s.replace(anchor,method+anchor,1)
source.write_text(s)
assert 'showGen1X3(dex,variant,root)' in s
assert 'copyTriple(originalVariantPrompt(dex,variant,lifestyle,true,root))' in s
print("Gen1 x3 chooser installed, x1 and other generations unchanged")
