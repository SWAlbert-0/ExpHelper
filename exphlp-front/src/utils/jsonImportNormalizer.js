function parseRoot(text) {
  let root;
  try {
    root = JSON.parse(text);
  } catch (error) {
    throw new Error(`JSON解析失败: ${error.message}`);
  }
  if (Array.isArray(root)) {
    return root;
  }
  if (root && typeof root === "object" && Array.isArray(root.items)) {
    return root.items;
  }
  if (root && typeof root === "object") {
    return [root];
  }
  throw new Error("JSON结构不合法，需为对象、数组或包含items数组的对象");
}

function trimValue(value) {
  if (value === null || value === undefined) {
    return "";
  }
  return String(value).trim();
}

function firstNonEmpty(obj, keys) {
  for (let i = 0; i < keys.length; i += 1) {
    const key = keys[i];
    if (!Object.prototype.hasOwnProperty.call(obj, key)) {
      continue;
    }
    const value = trimValue(obj[key]);
    if (value) {
      return { value, key };
    }
  }
  return { value: "", key: "" };
}

function buildProblemDescription(baseDescription, item) {
  const parts = [];
  if (Object.prototype.hasOwnProperty.call(item, "nVars") && trimValue(item.nVars)) {
    parts.push(`nVars=${trimValue(item.nVars)}`);
  }
  if (Object.prototype.hasOwnProperty.call(item, "nObjs") && trimValue(item.nObjs)) {
    parts.push(`nObjs=${trimValue(item.nObjs)}`);
  }
  if (parts.length === 0) {
    return baseDescription;
  }
  if (!baseDescription) {
    return parts.join("; ");
  }
  return `${baseDescription}; ${parts.join("; ")}`;
}

export function normalizeProblemImportJson(rawText) {
  const items = parseRoot(rawText);
  const normalizedItems = [];
  const warnings = [];
  const errors = [];
  let legacyMappedCount = 0;

  items.forEach((rawItem, index) => {
    if (!rawItem || typeof rawItem !== "object" || Array.isArray(rawItem)) {
      errors.push(`第 ${index + 1} 项不是对象`);
      return;
    }
    const instName = firstNonEmpty(rawItem, ["instName", "probName"]);
    const categoryName = firstNonEmpty(rawItem, ["categoryName", "probClassName"]);
    const dirName = firstNonEmpty(rawItem, ["dirName", "catalogName"]);
    const machineName = firstNonEmpty(rawItem, ["machineName"]);
    const machineIp = firstNonEmpty(rawItem, ["machineIp", "ip"]);
    const description = firstNonEmpty(rawItem, ["description", "probDesc"]);

    if (!instName.value) {
      errors.push(`第 ${index + 1} 项缺少 instName`);
      return;
    }

    const legacyKeys = [instName.key, categoryName.key, dirName.key, machineIp.key, description.key]
      .filter((key) => ["probName", "probClassName", "catalogName", "ip", "probDesc"].includes(key));
    if (legacyKeys.length > 0) {
      legacyMappedCount += 1;
      warnings.push(`第 ${index + 1} 项使用旧字段: ${legacyKeys.join(", ")}`);
    }

    normalizedItems.push({
      instName: instName.value,
      categoryName: categoryName.value,
      dirName: dirName.value,
      machineName: machineName.value,
      machineIp: machineIp.value,
      description: buildProblemDescription(description.value, rawItem),
    });
  });

  return {
    jsonText: JSON.stringify(normalizedItems, null, 2),
    summary: {
      total: items.length,
      normalized: normalizedItems.length,
      legacyMappedCount,
      warnings,
      errors,
    },
  };
}

function normalizeDefPara(defPara, index) {
  const paraName = firstNonEmpty(defPara, ["paraName", "name", "key"]);
  const paraType = firstNonEmpty(defPara, ["paraType", "type"]);
  const paraValue = firstNonEmpty(defPara, ["paraValue", "value", "defaultValue"]);
  const description = firstNonEmpty(defPara, ["description", "desc"]);
  const currentType = paraType.value || "String";
  const currentValue = paraValue.value;

  return {
    paraId: index + 1,
    paraName: paraName.value,
    paraType: currentType,
    paraValue: currentValue,
    description: description.value,
  };
}

function parseDefParas(item) {
  const defParas = item.defParas || item.params || item.parameters || item.paraList;
  if (!Array.isArray(defParas)) {
    return [];
  }
  return defParas
    .filter((row) => row && typeof row === "object" && !Array.isArray(row))
    .map((row, index) => normalizeDefPara(row, index));
}

export function normalizeAlgorithmImportJson(rawText) {
  const items = parseRoot(rawText);
  const normalizedItems = [];
  const warnings = [];
  const errors = [];
  let legacyMappedCount = 0;

  items.forEach((rawItem, index) => {
    if (!rawItem || typeof rawItem !== "object" || Array.isArray(rawItem)) {
      errors.push(`第 ${index + 1} 项不是对象`);
      return;
    }
    const algName = firstNonEmpty(rawItem, ["algName", "algorithmName", "name"]);
    const serviceName = firstNonEmpty(rawItem, ["serviceName", "service", "service_name", "appName"]);
    const description = firstNonEmpty(rawItem, ["description", "algDesc", "desc"]);

    if (!algName.value) {
      errors.push(`第 ${index + 1} 项缺少 algName`);
      return;
    }
    if (!serviceName.value) {
      errors.push(`第 ${index + 1} 项缺少 serviceName`);
      return;
    }

    const usedLegacy = [algName.key, serviceName.key, description.key]
      .filter((key) => ["algorithmName", "name", "service", "service_name", "appName", "algDesc", "desc"].includes(key));
    if (usedLegacy.length > 0 || rawItem.params || rawItem.parameters || rawItem.paraList) {
      legacyMappedCount += 1;
      warnings.push(`第 ${index + 1} 项使用兼容字段映射`);
    }

    normalizedItems.push({
      algName: algName.value,
      serviceName: serviceName.value,
      description: description.value,
      defParas: parseDefParas(rawItem),
    });
  });

  return {
    jsonText: JSON.stringify(normalizedItems, null, 2),
    summary: {
      total: items.length,
      normalized: normalizedItems.length,
      legacyMappedCount,
      warnings,
      errors,
    },
  };
}
