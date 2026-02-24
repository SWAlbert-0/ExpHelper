export function formatTimestampToDateTime(timestamp) {
  const dateObject = new Date(timestamp);
  const options = {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hour12: false,
  };
  const formattedDate = new Intl.DateTimeFormat("zh-CN", options).format(dateObject);
  return formattedDate.replace(/\//g, "-");
}

export function getCurrentDateTime() {
  const currentDateTime = new Date();
  const options = {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hour12: false,
  };
  return new Intl.DateTimeFormat("zh-CN", options).format(currentDateTime).replace(/\//g, "-");
}

export function buildProbInstsTreeData(probInsts) {
  const categoryNames = [];
  const treeData = [];
  for (let i = 0; i < probInsts.length; i++) {
    categoryNames.push(probInsts[i].categoryName);
  }
  const uniqueCategoryNames = categoryNames.filter((value, index, self) => self.indexOf(value) === index);
  for (let i = 0; i < uniqueCategoryNames.length; i++) {
    treeData.push({ id: uniqueCategoryNames[i], label: uniqueCategoryNames[i], children: [] });
  }
  for (let i = 0; i < probInsts.length; i++) {
    const index = uniqueCategoryNames.indexOf(probInsts[i].categoryName);
    treeData[index].children.push({ id: probInsts[i].instId, label: probInsts[i].instName });
  }
  return treeData;
}

export function getCheckedProbInstIds(treeRef) {
  const selectedProbInsts = treeRef.getCheckedNodes();
  const probInstIds = [];
  for (let i = 0; i < selectedProbInsts.length; i++) {
    if (selectedProbInsts[i].children == null) {
      probInstIds.push(selectedProbInsts[i].id);
    }
  }
  return probInstIds;
}

export function handleSameAlgName(algRunInfos) {
  const algIds = {};
  const algNames = [];
  for (let i = 0; i < algRunInfos.length; i++) {
    if (!Object.prototype.hasOwnProperty.call(algIds, algRunInfos[i].algId)) {
      algIds[algRunInfos[i].algId] = [];
    }
    algIds[algRunInfos[i].algId].push(i);
  }
  for (const key in algIds) {
    if (Object.prototype.hasOwnProperty.call(algIds, key)) {
      if (algIds[key].length > 1) {
        for (let i = 0; i < algIds[key].length; i++) {
          algNames[algIds[key][i]] = `${algRunInfos[algIds[key][i]].algName}-${i + 1}`;
        }
      } else {
        algNames[algIds[key]] = algRunInfos[algIds[key]].algName;
      }
    }
  }
  return algNames;
}

export function hasDuplicateParas(algRunInfos) {
  const arr = [];
  for (let i = 0; i < algRunInfos.length; i++) {
    arr.push({ runParas: algRunInfos[i].runParas, algName: algRunInfos[i].algName });
  }
  const seen = new Set();
  for (const obj of arr) {
    const objString = JSON.stringify(obj);
    if (seen.has(objString)) {
      return true;
    }
    seen.add(objString);
  }
  return false;
}
