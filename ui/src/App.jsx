import { useState } from "react"
import Rule from "./Rule"
import convertToFlagdFormat from "./convertToFlagdFormat"
import validateFlagdSchema from "./validateFlagdSchema"
import "./App.css"

const API_BASE_URL = "http://localhost:9090"

function App() {
  const [flagKey, setFlagKey] = useState("test-feature")
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

  const generateJSON = () => {
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
    const sourceId = "2979bd38-fa60-4e3d-8e52-b363cdc80082"
    const flagId = flagKey
    
    const flagdJson = JSON.parse(generateJSON())
    const flagData = flagdJson[flagKey]
    
    const requestBody = {
      name: flagKey,
      description: description || null,
      state: flagData.state,
      defaultVariant: flagData.defaultVariant || null,
      variants: flagData.variants || null,
      targeting: flagData.targeting || null
    }
    
    try {
      const response = await fetch(`${API_BASE_URL}/api/v1/sources/${sourceId}/flags/${flagId}`, {
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
    } catch (error) {
      console.error("Error saving flag:", error)
      setSaveResult({ success: false, message: "Error saving flag: " + error.message })
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

  return (
    <div className="app-container">
      <header className="app-header">
        <h1>flagd ui</h1>
        <div className="header-actions"></div>
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

export default App