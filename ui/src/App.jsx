import { useState, useMemo, useEffect, useCallback } from "react"
import { useParams, useNavigate } from "react-router-dom"
import { FlagdCore, MemoryStorage } from "@openfeature/flagd-core"
import Rule from "./Rule"
import convertToFlagdFormat from "./convertToFlagdFormat"
import validateFlagdSchema from "./validateFlagdSchema"
import convertFromFlagdFormat from "./convertFromFlagdFormat"
import "./App.css"

const API_BASE_URL = "http://localhost:9090"

function FlagEdit() {
  const { sourceId, flagId } = useParams()
  const navigate = useNavigate()
  
  const [flagKey, setFlagKey] = useState("new-flag")
  const [description, setDescription] = useState("")
  const [state, setState] = useState(true)
  const [type, setType] = useState("boolean")
  const [variants, setVariants] = useState([
    { name: "true", value: true },
    { name: "false", value: false }
  ])
  const [defaultVariant, setDefaultVariant] = useState("false")

  const [hasTargeting, setHasTargeting] = useState(false)
  const [rules, setRules] = useState([{
    condition: { name: "", operator: "ends_with", subOperator: ">=", value: "" },
    targetVariant: "true"
  }])
  const [hasDefaultRule, setHasDefaultRule] = useState(false)
  const [defaultRule, setDefaultRule] = useState("false")

  const [validationResult, setValidationResult] = useState(null)
  const [saveResult, setSaveResult] = useState(null)
  const [evaluationContext, setEvaluationContext] = useState("{}")
  const [evaluationResult, setEvaluationResult] = useState(null)
  const [validEvaluationContext, setValidEvaluationContext] = useState(true)
  const [loading, setLoading] = useState(flagId !== "new")
  const [source, setSource] = useState(null)

  useEffect(() => {
    const loadData = async () => {
      if (sourceId) {
        await fetchSource()
      }
      if (flagId && flagId !== "new") {
        await fetchFlag()
      } else {
        setLoading(false)
      }
    }
    loadData()
  }, [sourceId, flagId])

  const fetchSource = useCallback(async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/api/v1/sources/${sourceId}`)
      if (response.ok) {
        const sourceData = await response.json()
        setSource(sourceData)
      }
    } catch (err) {
      console.error("Error fetching source:", err)
    }
  }, [sourceId])

  const fetchFlag = useCallback(async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/api/v1/sources/${sourceId}/flags/${flagId}`)
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }
      const flagData = await response.json()
      loadFlagData(flagData)
    } catch (err) {
      console.error("Error fetching flag:", err)
      setSaveResult({ success: false, message: "Error loading flag: " + err.message })
    } finally {
      setLoading(false)
    }
  }, [sourceId, flagId])

  const loadFlagData = useCallback((flagData) => {
    const flagKey = flagData.flagId || flagData.name || flagId
    
    const variantsObj = flagData.variants || {}
    const variantEntries = Object.entries(variantsObj)
    
    const inferType = (entries) => {
      if (entries.length === 0) return "string"
      const firstValue = entries[0][1]
      if (typeof firstValue === "boolean") return "boolean"
      if (typeof firstValue === "number") return "number"
      if (typeof firstValue === "object" && firstValue !== null) return "object"
      return "string"
    }
    
    const type = inferType(variantEntries)
    const variants = variantEntries.map(([name, value]) => ({
      name,
      value: type === "object" ? JSON.stringify(value) : value
    }))
    
    const defaultVariant = flagData.defaultVariant || variants[0]?.name || ""
    
    const targeting = flagData.targeting || {}
    const hasIf = targeting.if && Array.isArray(targeting.if) && targeting.if.length > 0
    const hasTargeting = !!hasIf
    
    let rules = []
    let hasDefaultRule = false
    let defaultRule = variants[0]?.name || ""
    
    if (hasTargeting) {
      const ifArray = targeting.if
      const hasOddElements = ifArray.length % 2 === 1
      const pairsToProcess = hasOddElements ? ifArray.length - 1 : ifArray.length
      
      for (let i = 0; i < pairsToProcess; i += 2) {
        const condition = ifArray[i]
        const targetVariant = ifArray[i + 1]
        
        const parsedCondition = parseCondition(condition)
        rules.push({
          condition: parsedCondition,
          targetVariant
        })
      }
      
      if (hasOddElements) {
        hasDefaultRule = true
        defaultRule = ifArray[ifArray.length - 1]
      }
    }
    
    function parseCondition(condition) {
      const result = {
        name: "",
        operator: "ends_with",
        subOperator: ">=",
        value: ""
      }
      
      if (!condition || typeof condition !== 'object') {
        return result
      }
      
      if (condition["!"]) {
        const innerCondition = condition["!"]
        const innerOperator = Object.keys(innerCondition)[0]
        const innerOperands = innerCondition[innerOperator]
        
        if (innerOperands && Array.isArray(innerOperands) && innerOperands[0] && innerOperands[0].var) {
          result.name = innerOperands[0].var
        }
        
        if (innerOperator === "in") {
          if (Array.isArray(innerOperands[1])) {
            result.operator = "not_in_list"
            result.value = innerOperands[1].join(", ")
          } else {
            result.operator = "not_in_string"
            result.value = innerOperands[1] || ""
          }
        }
        return result
      }
      
      const operators = Object.keys(condition)
      if (operators.length === 0) {
        return result
      }
      
      const operator = operators[0]
      const operands = condition[operator]
      
      if (!Array.isArray(operands)) {
        return result
      }
      
      if (operands[0] && operands[0].var) {
        result.name = operands[0].var
      }
      
      if (operator === "sem_ver") {
        result.operator = "sem_ver"
        result.subOperator = operands[1] || ">="
        result.value = operands[2] || ""
        return result
      }
      
      if (operator === "in") {
        if (Array.isArray(operands[1])) {
          result.operator = "in_list"
          result.value = operands[1].join(", ")
        } else {
          result.operator = "in_string"
          result.value = operands[1] || ""
        }
        return result
      }
      
      result.operator = operator
      result.value = operands[1] !== undefined ? String(operands[1]) : ""
      
      return result
    }
    
    setFlagKey(flagKey)
    setDescription(flagData.description || "")
    setState(flagData.state === "ENABLED")
    setType(type)
    setVariants(variants.length > 0 ? variants : [
      { name: "true", value: true },
      { name: "false", value: false }
    ])
    setDefaultVariant(defaultVariant)
    setHasTargeting(hasTargeting)
    setRules(rules.length > 0 ? rules : [{
      condition: { name: "", operator: "ends_with", subOperator: ">=", value: "" },
      targetVariant: variants[0]?.name || "true"
    }])
    setHasDefaultRule(hasDefaultRule)
    setDefaultRule(defaultRule)
  }, [flagId])

  const generateJSON = useCallback(() => {
    const json = {
      flagKey,
      state,
      type,
      variants,
      defaultVariant,
      hasTargeting,
      rules,
      hasDefaultRule,
      defaultRule
    }
    const convertedJson = convertToFlagdFormat(json)
    return JSON.stringify(convertedJson, null, 2)
  }, [flagKey, state, type, variants, defaultVariant, hasTargeting, rules, hasDefaultRule, defaultRule])

  const flagStorage = useMemo(() => new MemoryStorage(console), [])
  const flagdCore = useMemo(
    () => new FlagdCore(flagStorage, console),
    [flagStorage]
  )

  useEffect(() => {
    try {
      const flagdJsonString = generateJSON()
      console.log("Type of flagdJsonString:", typeof flagdJsonString)
      console.log("flagdJsonString:", flagdJsonString)
      flagdCore.setConfigurations(flagdJsonString)
      console.log("Available flags:", Array.from(flagdCore.getFlags().keys()))
    } catch (err) {
      console.error("Error setting flagd configuration:", err)
    }
  }, [generateJSON, flagdCore])

  useEffect(() => {
    try {
      JSON.parse(evaluationContext)
      setValidEvaluationContext(true)
    } catch (err) {
      setValidEvaluationContext(false)
    }
  }, [evaluationContext])

  const handleTypeChange = (newType) => {
    let newVariants
    let newDefaultVariant

    if (newType === "boolean") {
      newVariants = [
        { name: "true", value: true },
        { name: "false", value: false }
      ]
      newDefaultVariant = "false"
    } else if (newType === "string") {
      newVariants = [
        { name: "foo", value: "foo" },
        { name: "bar", value: "bar" }
      ]
      newDefaultVariant = "foo"
    } else if (newType === "number") {
      newVariants = [
        { name: "1", value: 1 },
        { name: "2", value: 2 }
      ]
      newDefaultVariant = "1"
    } else if (newType === "object") {
      newVariants = [
        { name: "foo", value: JSON.stringify({ foo: "foo" }) },
        { name: "bar", value: JSON.stringify({ bar: "bar" }) }
      ]
      newDefaultVariant = "foo"
    }

    setType(newType)
    setVariants(newVariants)
    setDefaultVariant(newDefaultVariant)
    setDefaultRule(newDefaultVariant)
    setRules([{
      condition: { name: "", operator: "ends_with", subOperator: ">=", value: "" },
      targetVariant: newDefaultVariant
    }])
  }

  const handleVariantChange = (index, key, value) => {
    const newVariants = variants.map((variant, i) => {
      if (i === index) {
        return { ...variant, [key]: value }
      }
      return variant
    })
    setVariants(newVariants)
    newVariants.length === 1 ? setDefaultVariant(newVariants[0].name) : null
  }

  const addVariant = () => {
    const newVariant = { name: "" }
    if (type === "boolean") {
      newVariant.value = false
    } else if (type === "string") {
      newVariant.value = ""
    } else if (type === "number") {
      newVariant.value = 0
    } else if (type === "object") {
      newVariant.value = JSON.stringify({})
    }
    setVariants([...variants, newVariant])
  }

  const removeVariant = (index) => {
    const newVariants = variants.filter((_, i) => i !== index)
    setVariants(newVariants)
    if (defaultVariant === variants[index].name) {
      setDefaultVariant(newVariants[0]?.name || "");
    }
  }

  const handleRuleChange = (index, key, value) => {
    const newRules = rules.map((rule, i) => {
      if (i === index) {
        if (key !== "") {
          return { condition: { ...rule.condition, [key]: value }, targetVariant: rule.targetVariant }
        } else {
          return { condition: rule.condition, targetVariant: value }
        }
      }
      return rule
    })
    setRules(newRules)
  }

  const addRule = () => {
    const newRule = {
      condition: { name: "", operator: "ends_with", subOperator: ">=", value: "" },
      targetVariant: "true"
    }
    setRules([...rules, newRule])
  }

  const removeRule = (index) => {
    const newRules = rules.filter((_, i) => i !== index)
    setRules(newRules)
  }

  const handleValidate = () => {
    try {
      const json = JSON.parse(generateJSON())
      const result = validateFlagdSchema(json)
      setValidationResult(result)
    } catch (e) {
      setValidationResult({ valid: false, errors: [`JSON error: ${e.message}`] })
    }
  }

  const handleSave = async () => {
    if (!sourceId) {
      setSaveResult({ success: false, message: "No source selected" })
      return
    }

    const currentFlagId = flagId === "new" ? flagKey : flagId
    const flagdJson = JSON.parse(generateJSON())
    const flagData = flagdJson.flags[flagKey]
    
    const requestBody = {
      name: flagKey,
      description: description || null,
      state: flagData.state,
      defaultVariant: flagData.defaultVariant || null,
      variants: flagData.variants || null,
      targeting: flagData.targeting || null
    }
    
    try {
      const response = await fetch(`${API_BASE_URL}/api/v1/sources/${sourceId}/flags/${currentFlagId}`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(requestBody)
      })
      
      if (!response.ok) {
        const errorText = await response.text()
        throw new Error(`HTTP error! status: ${response.status}, ${errorText}`)
      }
      
      setSaveResult({ success: true, message: "Flag saved successfully" })
      if (flagId === "new") {
        navigate(`/sources/${sourceId}/flags/${flagKey}`, { replace: true })
      }
    } catch (error) {
      console.error("Error saving flag:", error)
      setSaveResult({ success: false, message: "Error saving flag: " + error.message })
    }
  }

  const handleBack = () => {
    if (sourceId) {
      navigate(`/sources/${sourceId}/flags`)
    } else {
      navigate("/")
    }
  }

  const handleEvaluate = () => {
    try {
      const context = JSON.parse(evaluationContext)
      let result
      switch (type) {
        case "boolean":
          result = flagdCore.resolveBooleanEvaluation(flagKey, false, context, console)
          break
        case "string":
          result = flagdCore.resolveStringEvaluation(flagKey, "", context, console)
          break
        case "number":
          result = flagdCore.resolveNumberEvaluation(flagKey, 0, context, console)
          break
        case "object":
          result = flagdCore.resolveObjectEvaluation(flagKey, {}, context, console)
          break
      }
      setEvaluationResult({ success: true, value: result })
    } catch (error) {
      setEvaluationResult({ success: false, error: error.message })
    }
  }

  const getBooleanVariantBlock = (variant, index) => ( type === "boolean" ?
    <select id={`variant${index}Value`} className="select" value={variant.value.toString()}
      onChange={(e) => handleVariantChange(index, "value", e.target.value === "true")}>
      <option value="true">true</option>
      <option value="false">false</option>
    </select> : null )

  const getStringVariantBlock = (variant, index) => ( type === "string" ?
    <input id={`variant${index}Value`} className="input" placeholder="Value" value={variant.value}
      onChange={(e) => handleVariantChange(index, "value", e.target.value)} /> : null )

  const getNumberVariantBlock = (variant, index) => ( type === "number" ?
    <input id={`variant${index}Value`} className="input" type="number" value={variant.value}
      onChange={(e) => handleVariantChange(index, "value", Number(e.target.value))} /> : null )

  const getObjectVariantBlock = (variant, index) => ( type === "object" ?
    <input id={`variant${index}Value`} className="input" value={variant.value}
      onChange={(e) => handleVariantChange(index, "value", e.target.value)} /> : null )

  const variantsBlock = variants.map((variant, index) => (
    <div key={`variant${index}`} className="variant-item">
      <input id={`variant${index}Name`} className="input" placeholder="Name" value={variant.name}
        onChange={(e) => handleVariantChange(index, "name", e.target.value)} />
      {getBooleanVariantBlock(variant, index)}
      {getStringVariantBlock(variant, index)}
      {getNumberVariantBlock(variant, index)}
      {getObjectVariantBlock(variant, index)}
      <button id="removeVariant" className="button button-danger" onClick={() => removeVariant(index)}>Remove</button>
    </div>
  ))

  const variantOptionsBlock = variants.filter(variant => variant.name).map((variant, index) => (
    <option key={`variant-${index}`} value={variant.name}>{variant.name}</option>
  ))

  const rulesBlock = hasTargeting && rules.map((rule, index) => (
    <Rule key={index} index={index} variants={variants} rule={rule}
      handleRuleChange={handleRuleChange} removeRule={() => removeRule(index)} />
  ))

  const defaultRuleBlock = hasTargeting && hasDefaultRule && (
    <div className="default-rule-section">
      <label>Else</label>
      <select id="defaultRule" className="select"
        value={defaultRule}
        onChange={(e) => setDefaultRule(e.target.value)}>
        {variantOptionsBlock}
      </select>
    </div>
  )

  const addRuleButton = hasTargeting && (
    <button id="addRule" className="button button-primary" onClick={() => addRule()}>Add Rule</button>
  )

  const defaultRuleCheckbox = hasTargeting && (
    <label className="checkbox-wrapper">
      <input id="defaultRule" className="checkbox" type="checkbox" checked={hasDefaultRule}
        onClick={(e) => setHasDefaultRule(e.target.checked)} />
      <span>Default Rule</span>
    </label>
  )

  const validationBlock = validationResult && (
    <div className={`validation-result ${validationResult.valid ? 'validation-success' : 'validation-error'}`}>
      <div className="validation-header">
        {validationResult.valid ? 'Valid' : 'Invalid'}
      </div>
      {!validationResult.valid && validationResult.errors.length > 0 && (
        <ul className="validation-errors">
          {validationResult.errors.map((error, index) => (
            <li key={index}>{error}</li>
          ))}
        </ul>
      )}
      <button className="validation-dismiss" onClick={() => setValidationResult(null)}>&times;</button>
    </div>
  )

  const saveResultBlock = saveResult && (
    <div className={`validation-result ${saveResult.success ? 'validation-success' : 'validation-error'}`}>
      <div className="validation-header">
        {saveResult.success ? 'Success' : 'Error'}
      </div>
      <div className="validation-errors">
        {saveResult.message}
      </div>
      <button className="validation-dismiss" onClick={() => setSaveResult(null)}>&times;</button>
    </div>
  )

  const evaluationResultBlock = evaluationResult && (
    <div className={`validation-result ${evaluationResult.success ? 'validation-success' : 'validation-error'}`}>
      <div className="validation-header">
        {evaluationResult.success ? 'Evaluation Result' : 'Error'}
      </div>
      <div className="validation-errors">
        {evaluationResult.success ? (
          typeof evaluationResult.value === 'object' 
            ? <pre style={{ margin: 0, whiteSpace: 'pre-wrap', fontFamily: 'monospace', fontSize: '14px' }}>{JSON.stringify(evaluationResult.value, null, 2)}</pre>
            : String(evaluationResult.value)
        ) : (
          evaluationResult.error
        )}
      </div>
      <button className="validation-dismiss" onClick={() => setEvaluationResult(null)}>&times;</button>
    </div>
  )

  if (loading) {
    return (
      <div className="app-container">
        <header className="app-header">
          <h1>{flagId === "new" ? "New Flag" : "Edit Flag"}</h1>
        </header>
        <div className="loading">Loading...</div>
      </div>
    )
  }

  return (
    <div className="app-container">
      <header className="app-header">
        <div className="header-breadcrumb">
          <button className="breadcrumb-link" onClick={handleBack}>Sources</button>
          <span className="breadcrumb-separator">/</span>
          <button className="breadcrumb-link" onClick={handleBack}>{source?.name || 'Source'}</button>
          <span className="breadcrumb-separator">/</span>
          <span className="breadcrumb-current">{flagId === "new" ? "New Flag" : flagKey}</span>
        </div>
      </header>
      <div className="app-layout">
        <div className="form-panel">
          <div className="form-section">
            <span className="section-header">Flag Configuration</span>
            <div className="form-group">
              <label htmlFor="flagKey" className="form-label">Flag Key</label>
              <input id="flagKey" className="input" value={flagKey}
                onChange={(e) => setFlagKey(e.target.value)} />
            </div>
            <div className="form-group">
              <label htmlFor="description" className="form-label">Description</label>
              <input id="description" className="input" value={description}
                onChange={(e) => setDescription(e.target.value)} />
            </div>
            <div className="form-group">
              <label className="checkbox-wrapper">
                <input id="state" className="checkbox" type="checkbox" checked={state}
                  onChange={(e) => setState(e.target.checked)} />
                <span>Enabled</span>
              </label>
            </div>
            <div className="form-group">
              <label htmlFor="type" className="form-label">Type</label>
              <select id="type" className="select" value={type}
                onChange={(e) => handleTypeChange(e.target.value)}>
                <option value="boolean">boolean</option>
                <option value="string">string</option>
                <option value="number">number</option>
                <option value="object">object</option>
              </select>
            </div>
          </div>
          <div className="form-section">
            <span className="section-header">Variants</span>
            <div className="variant-list">
              {variantsBlock}
            </div>
            <button id="addVariant" className="button button-secondary" onClick={addVariant}>Add Variant</button>
          </div>

          <div className="form-section">
            <span className="section-header">Default Variant</span>
            <div className="form-group">
              <select id="defaultVariant" className="select"
                value={defaultVariant}
                onChange={(e) => setDefaultVariant(e.target.value)}>
                {variantOptionsBlock}
              </select>
            </div>
          </div>
          <div className="form-section">
            <span className="section-header">Targeting</span>
            <div className="targeting-section">
              <label className="checkbox-wrapper">
                <input id="hasTargeting" className="checkbox" type="checkbox" checked={hasTargeting}
                  onChange={(e) => setHasTargeting(e.target.checked)} />
                <span>Enable Targeting</span>
              </label>
              {hasTargeting && (
                <>
                  <div className="rules-container">
                    {rulesBlock}
                  </div>
                  {defaultRuleBlock}
                  <div className="action-buttons">
                    {addRuleButton}
                    {defaultRuleCheckbox}
                  </div>
                </>
              )}
            </div>
          </div>
          <div className="form-section">
            <span className="section-header">Evaluation</span>
            <div className="targeting-section">
              <div className="form-group">
                <label htmlFor="evaluationContext" className="form-label">Evaluation Context (JSON)</label>
                <textarea 
                  id="evaluationContext" 
                  className={validEvaluationContext ? '' : 'input--error'}
                  value={evaluationContext}
                  onChange={(e) => setEvaluationContext(e.target.value)}
                  rows={6}
                  style={{ 
                    fontFamily: 'monospace', 
                    fontSize: '14px', 
                    resize: 'none',
                    width: '100%',
                    padding: 'var(--spacing-3)',
                    border: 'var(--border-width-thin) solid var(--input-border)',
                    borderRadius: 'var(--radius-sm)',
                    background: 'var(--input-background)',
                    color: 'var(--input-text)'
                  }}
                />
                {!validEvaluationContext && (
                  <div className="form-error">Invalid JSON format</div>
                )}
              </div>
              <button 
                id="evaluateButton" 
                className="button button-primary" 
                onClick={handleEvaluate}
                disabled={!validEvaluationContext}
              >
                Evaluate
              </button>
              {evaluationResultBlock}
            </div>
          </div>
        </div>

        <div className="json-panel">
          <div className="json-panel-header">
            <span className="json-panel-title">Output</span>
            <div className="json-panel-actions">
              <button className="button button-secondary" onClick={handleValidate}>Validate</button>
              <button className="button button-primary" onClick={handleSave}>Save</button>
            </div>
          </div>
          {validationBlock}
          {saveResultBlock}
          <textarea id="json" className="json-textarea" readOnly value={generateJSON()} rows={30} />
        </div>
      </div>
    </div>
  )
}

export default FlagEdit