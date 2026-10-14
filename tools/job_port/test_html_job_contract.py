from pathlib import Path
from extract_html_job_contract import extract
import sys

def check(path):
    c, rows = extract(path)
    dex = {r["dex"]: r for r in rows}
    assert len(rows) == 1025
    assert c["schema"] == "NOVARIS_JOB_ASSIGNMENTS_v16"
    assert len(c["V2610_RAG"]) == 19
    assert len(c["V2610_CIV"]) == 60
    assert sum(len(v["specializations"]) for v in c["V2610_CIV"].values()) == 241
    assert len(c["NOVARIS_ASSIGNMENT_OVERRIDES_V2645"]) == 287
    assert [dex[n]["role"] for n in (4,5,6)] == ["Blacksmith","Mastersmith","Mechanic"]
    assert dex[134]["original_override"]["id"] == "civil:Professional Diver"
    assert dex[135]["original_override"]["id"] == "civil:Electrician"
    print("V27.42 canonical job contract PASS")

if __name__ == "__main__":
    check(Path(sys.argv[1]))
